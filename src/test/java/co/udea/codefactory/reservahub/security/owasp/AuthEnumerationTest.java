package co.udea.codefactory.reservahub.security.owasp;

import co.udea.codefactory.reservahub.AbstractIntegrationTest;
import co.udea.codefactory.reservahub.TestApiClient;
import co.udea.codefactory.reservahub.audit.AuditLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static co.udea.codefactory.reservahub.security.owasp.OwaspTestSupport.mvc;
import static co.udea.codefactory.reservahub.security.owasp.OwaspTestSupport.remoteAddr;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * V-04 y V-05 · Autenticación sin enumeración ni bloqueo abusable.
 *
 * <ul>
 *   <li>Login: email inexistente, clave errónea y pareja email+IP bloqueada responden lo mismo (401, mismo
 *   cuerpo salvo {@code traceId}, sin {@code Retry-After}).</li>
 *   <li>El bloqueo es por pareja email+IP: los fallos desde la IP A no impiden entrar desde la IP B.</li>
 *   <li>Registro duplicado (email de cliente, email o nombre de proveedor): 409 con un mensaje genérico.</li>
 *   <li>{@code GET /users/lookup} deja evento {@code CLIENT_LOOKUP} con actor, resultado y hash del email.</li>
 * </ul>
 */
@SpringBootTest
class AuthEnumerationTest extends AbstractIntegrationTest {

	private static final String LOGIN = "/api/v1/auth/login";
	private static final String PASSWORD = "Secret123";
	private static final String GENERIC_REGISTRATION = "No se pudo completar el registro con los datos enviados";
	private static final String IP_A = "203.0.113.10";
	private static final String IP_B = "203.0.113.20";

	@Autowired
	private ApplicationContext context;
	@MockitoSpyBean
	private AuditLogger auditLogger;

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = mvc(context);
	}

	@Test
	void unknownEmailWrongPasswordAndLockedPairLookIdentical() throws Exception {
		String email = registerClient();

		MockHttpServletResponse unknown = login("nadie-" + UUID.randomUUID() + "@example.com", "Wrong999", IP_A);
		MockHttpServletResponse wrong = login(email, "Wrong999", IP_A);
		for (int i = 0; i < 4; i++) {
			login(email, "Wrong999", IP_A);
		}
		MockHttpServletResponse locked = login(email, PASSWORD, IP_A);

		for (MockHttpServletResponse r : List.of(unknown, wrong, locked)) {
			assertEquals(401, r.getStatus(), r.getContentAsString());
			assertNull(r.getHeader("Retry-After"), "el 401 no debe revelar el bloqueo con Retry-After");
		}
		assertEquals(withoutTraceId(unknown), withoutTraceId(wrong));
		assertEquals(withoutTraceId(unknown), withoutTraceId(locked));
	}

	@Test
	void failuresFromOneIpDoNotLockTheAccountForAnotherIp() throws Exception {
		String email = registerClient();
		for (int i = 0; i < 5; i++) {
			assertEquals(401, login(email, "Wrong999", IP_A).getStatus());
		}

		assertEquals(200, login(email, PASSWORD, IP_B).getStatus(), "la víctima entra desde su propia IP");
		assertEquals(401, login(email, PASSWORD, IP_A).getStatus(), "la IP atacante sigue frenada");

		ArgumentCaptor<String> details = ArgumentCaptor.forClass(String.class);
		verify(auditLogger, atLeastOnce()).info(org.mockito.ArgumentMatchers.eq("LOGIN_LOCKED"), details.capture());
		assertTrue(details.getAllValues().stream().anyMatch(d -> d.contains("ip=" + IP_A)), details.getAllValues().toString());
	}

	@Test
	void duplicateRegistrationsGetTheSameGenericConflict() throws Exception {
		String email = registerClient();
		MockHttpServletResponse dupClient = send("/api/v1/users", clientJson(email));

		String suffix = UUID.randomUUID().toString().substring(0, 8);
		String business = "Negocio " + suffix;
		assertEquals(201, send("/api/v1/providers", providerJson("p1-" + suffix + "@example.com", business)).getStatus());
		MockHttpServletResponse dupName = send("/api/v1/providers", providerJson("p2-" + suffix + "@example.com", business));
		MockHttpServletResponse dupProviderEmail = send("/api/v1/providers", providerJson(email, "Otro " + suffix));

		for (MockHttpServletResponse r : List.of(dupClient, dupName, dupProviderEmail)) {
			assertEquals(409, r.getStatus(), r.getContentAsString());
			assertTrue(r.getContentAsString().contains(GENERIC_REGISTRATION), r.getContentAsString());
		}
		assertEquals(withoutTraceId(dupClient), withoutTraceId(dupName));

		ArgumentCaptor<String> details = ArgumentCaptor.forClass(String.class);
		verify(auditLogger, atLeastOnce()).info(org.mockito.ArgumentMatchers.eq("REGISTRATION_REJECTED"), details.capture());
		assertTrue(details.getAllValues().stream().anyMatch(d -> d.contains("ip=")), details.getAllValues().toString());
		assertFalse(details.getAllValues().stream().anyMatch(d -> d.contains(email)), "sin email en claro");
	}

	@Test
	void clientLookupIsAudited() throws Exception {
		String token = TestApiClient.registerProviderAndLogin(mvc);
		String email = registerClient();
		String missing = "nadie-" + UUID.randomUUID() + "@example.com";

		assertEquals(200, mvc.perform(get("/api/v1/users/lookup").param("email", email)
				.header("Authorization", "Bearer " + token)).andReturn().getResponse().getStatus());
		assertEquals(404, mvc.perform(get("/api/v1/users/lookup").param("email", missing)
				.header("Authorization", "Bearer " + token)).andReturn().getResponse().getStatus());

		ArgumentCaptor<String> details = ArgumentCaptor.forClass(String.class);
		verify(auditLogger, atLeastOnce()).info(org.mockito.ArgumentMatchers.eq("CLIENT_LOOKUP"), details.capture());
		List<String> all = details.getAllValues();
		String foundHash = sha256(email);
		String missingHash = sha256(missing);
		assertTrue(all.stream().anyMatch(d -> d.contains("actor=") && d.contains("found=true")
				&& d.contains("emailHash=" + foundHash)), all.toString());
		assertTrue(all.stream().anyMatch(d -> d.contains("found=false")
				&& d.contains("emailHash=" + missingHash)), all.toString());
		assertFalse(all.stream().anyMatch(d -> d.contains(email) || d.contains(missing)), "sin email en claro");
	}

	private String registerClient() throws Exception {
		String email = "enum-" + UUID.randomUUID() + "@example.com";
		assertEquals(201, send("/api/v1/users", clientJson(email)).getStatus());
		return email;
	}

	private MockHttpServletResponse login(String email, String password, String ip) throws Exception {
		return mvc.perform(post(LOGIN).with(remoteAddr(ip)).contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
				.andReturn().getResponse();
	}

	private MockHttpServletResponse send(String path, String json) throws Exception {
		return mvc.perform(post(path)
				.with(remoteAddr(IP_A)).contentType(MediaType.APPLICATION_JSON).content(json))
				.andReturn().getResponse();
	}

	private static String clientJson(String email) {
		return """
				{"email":"%s","password":"%s","firstName":"Ana","lastName":"Pérez"}
				""".formatted(email, PASSWORD);
	}

	private static String providerJson(String email, String business) {
		return """
				{"email":"%s","password":"%s","firstName":"Ana","lastName":"Prov",
				 "businessName":"%s","serviceType":"Salud"}
				""".formatted(email, PASSWORD, business);
	}

	private static String withoutTraceId(MockHttpServletResponse r) throws Exception {
		return r.getContentAsString(StandardCharsets.UTF_8).replaceAll("\"traceId\"\\s*:\\s*(\"[^\"]*\"|null)", "");
	}

	private static String sha256(String email) throws Exception {
		byte[] d = MessageDigest.getInstance("SHA-256").digest(email.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
		return HexFormat.of().formatHex(d);
	}
}
