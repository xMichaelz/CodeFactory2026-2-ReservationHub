package co.udea.codefactory.reservahub.security.owasp;

import co.udea.codefactory.reservahub.AbstractIntegrationTest;
import co.udea.codefactory.reservahub.security.jwt.JwtService;
import co.udea.codefactory.reservahub.shared.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;

import static co.udea.codefactory.reservahub.security.owasp.OwaspTestSupport.authenticationFor;
import static co.udea.codefactory.reservahub.security.owasp.OwaspTestSupport.bearerFor;
import static co.udea.codefactory.reservahub.security.owasp.OwaspTestSupport.discoverEndpoints;
import static co.udea.codefactory.reservahub.security.owasp.OwaspTestSupport.mvc;
import static co.udea.codefactory.reservahub.security.owasp.OwaspTestSupport.remoteAddr;
import static co.udea.codefactory.reservahub.security.owasp.OwaspTestSupport.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/**
 * V-03 · Limitación de tasa: todo endpoint público o costoso responde 429 al pasarse del límite.
 *
 * <p>Contrato de propiedades (lo fija O1, lo implementa O4):
 * <ul>
 *   <li>{@code app.rate-limit.enabled} — activa el límite.</li>
 *   <li>{@code app.rate-limit.login-per-minute} — por IP, {@code POST /api/v1/auth/login} (prod 10).</li>
 *   <li>{@code app.rate-limit.registration-per-hour} — por IP, {@code POST /api/v1/users} (prod 20).</li>
 *   <li>{@code app.rate-limit.provider-registration-per-hour} — por IP, {@code POST /api/v1/providers} (prod 3).</li>
 *   <li>{@code app.rate-limit.authenticated-per-minute} — por usuario: availability, catalog, reservations/mine (prod 60).</li>
 *   <li>{@code app.rate-limit.lookup-per-minute} — por usuario, {@code GET /api/v1/users/lookup} (prod 10).</li>
 * </ul>
 * La IP es la dirección remota de la petición. Al pasarse: 429, cabecera {@code Retry-After} y
 * {@code errorCode = RATE_LIMITED}. En {@code src/test/resources/application.properties} los límites van a
 * 10000 para no romper el resto de la suite; aquí se bajan a 2.
 *
 * <p>Público = el anónimo no recibe 401 (se descubre solo); exento solo {@code /actuator/health}.
 */
@SpringBootTest
@TestPropertySource(properties = {
		"app.rate-limit.enabled=true",
		"app.rate-limit.login-per-minute=2",
		"app.rate-limit.registration-per-hour=2",
		"app.rate-limit.provider-registration-per-hour=2",
		"app.rate-limit.authenticated-per-minute=2",
		"app.rate-limit.lookup-per-minute=2",
		// Las propiedades en línea pasan por Properties.load, por eso la barra del regex va doble.
		"app.rate-limit.trusted-proxies=172\\\\.(1[6-9]|2[0-9]|3[0-1])\\\\.\\\\d{1,3}\\\\.\\\\d{1,3}"
})
class RateLimitCoverageTest extends AbstractIntegrationTest {

	private static final int LIMIT = 2;
	private static final Set<String> EXEMPT = Set.of("GET /actuator/health");
	private static final String LOGIN = "POST /api/v1/auth/login";
	private static final String CATALOG = "GET /api/v1/catalog/services";
	private static final Pattern RATE_LIMITED = Pattern.compile("\"errorCode\"\\s*:\\s*\"RATE_LIMITED\"");

	/** Endpoints autenticados costosos → rol con el que se llaman. */
	private static final Map<String, Role> COSTLY = Map.of(
			"GET /api/v1/services/{serviceId}/availability", Role.CLIENT,
			"GET /api/v1/catalog/services", Role.CLIENT,
			"GET /api/v1/reservations/mine", Role.CLIENT,
			"GET /api/v1/users/lookup", Role.PROVIDER);

	@Autowired
	private ApplicationContext context;
	@Autowired
	private JwtService jwtService;

	/** Descubre los públicos con una IP propia, para no gastar el cupo de las IP de prueba. */
	private Set<String> publicEndpoints(MockMvc mvc) {
		Set<String> result = new TreeSet<>();
		for (String endpoint : discoverEndpoints(context)) {
			if (EXEMPT.contains(endpoint)) {
				continue;
			}
			int status = OwaspTestSupport.status(mvc, request(endpoint).with(remoteAddr("10.250.0.1")));
			if (status != 401) {
				result.add(endpoint);
			}
		}
		return result;
	}

	private static String checkThirdIs429(String endpoint, MockHttpServletResponse third) throws Exception {
		List<String> problems = new ArrayList<>();
		if (third.getStatus() != 429) {
			problems.add("status " + third.getStatus());
		}
		if (third.getHeader("Retry-After") == null) {
			problems.add("sin Retry-After");
		}
		if (!RATE_LIMITED.matcher(third.getContentAsString()).find()) {
			problems.add("sin errorCode RATE_LIMITED");
		}
		return problems.isEmpty() ? null : endpoint + " → " + String.join(", ", problems);
	}

	@Test
	void discoveryFindsTheKnownPublicEndpoints() {
		Set<String> found = publicEndpoints(mvc(context));
		assertTrue(found.containsAll(Set.of("POST /api/v1/auth/login", "POST /api/v1/users", "POST /api/v1/providers")),
				"Públicos descubiertos: " + found);
		assertTrue(discoverEndpoints(context).containsAll(COSTLY.keySet()),
				"La lista COSTLY tiene rutas que ya no existen: " + COSTLY.keySet());
	}

	@Test
	void everyPublicEndpointAnswers429AfterTheLimit() throws Exception {
		MockMvc mvc = mvc(context);
		List<String> failures = new ArrayList<>();
		int n = 0;
		for (String endpoint : publicEndpoints(mvc)) {
			String ip = "10.251.0." + (++n);
			MockHttpServletResponse last = null;
			for (int i = 0; i <= LIMIT; i++) {
				last = mvc.perform(request(endpoint).with(remoteAddr(ip))).andReturn().getResponse();
			}
			String problem = checkThirdIs429(endpoint, last);
			if (problem != null) {
				failures.add(problem);
			}
		}
		assertTrue(failures.isEmpty(), "Públicos sin límite de tasa: " + failures);
	}

	@Test
	void everyCostlyEndpointAnswers429AfterTheLimit() throws Exception {
		MockMvc mvc = mvc(context);
		List<String> failures = new ArrayList<>();
		int n = 0;
		for (Map.Entry<String, Role> row : COSTLY.entrySet()) {
			UUID user = UUID.randomUUID(); // un usuario por endpoint: los cupos por usuario no se mezclan
			String ip = "10.252.0." + (++n);
			MockHttpServletResponse last = null;
			for (int i = 0; i <= LIMIT; i++) {
				try {
					last = mvc.perform(request(row.getKey()).with(remoteAddr(ip))
							.header("Authorization", bearerFor(jwtService, user, row.getValue()))
							.with(authentication(authenticationFor(user, row.getValue()))))
							.andReturn().getResponse();
				} catch (Exception ex) {
					last = new MockHttpServletResponse();
					last.setStatus(500);
				}
			}
			String problem = checkThirdIs429(row.getKey(), last);
			if (problem != null) {
				failures.add(problem);
			}
		}
		assertTrue(failures.isEmpty(), "Costosos sin límite de tasa: " + failures);
	}

	private static int[] statuses(MockMvc mvc, int times, java.util.function.Supplier<
			org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder> builder) throws Exception {
		int[] result = new int[times];
		for (int i = 0; i < times; i++) {
			result[i] = mvc.perform(builder.get()).andReturn().getResponse().getStatus();
		}
		return result;
	}

	@Test
	void twoClientsBehindTheTrustedProxyHaveSeparateBuckets() throws Exception {
		MockMvc mvc = mvc(context);
		String proxy = "172.18.0.5";
		int[] first = statuses(mvc, LIMIT + 1, () -> request(LOGIN).with(remoteAddr(proxy))
				.header("X-Forwarded-For", "198.51.100.1"));
		int[] second = statuses(mvc, 1, () -> request(LOGIN).with(remoteAddr(proxy))
				.header("X-Forwarded-For", "198.51.100.2"));
		assertEquals(429, first[LIMIT], "El tercer intento del primer cliente debe dar 429");
		assertNotEquals(429, second[0], "El segundo cliente tras el mismo proxy tiene su propio cupo");
	}

	@Test
	void forgedForwardedForFromAnUntrustedAddressDoesNotEvadeTheLimit() throws Exception {
		MockMvc mvc = mvc(context);
		String attacker = "203.0.113.7";
		MockHttpServletResponse last = null;
		for (int i = 0; i <= LIMIT; i++) {
			last = mvc.perform(request(LOGIN).with(remoteAddr(attacker))
					.header("X-Forwarded-For", "198.51.100." + (50 + i))).andReturn().getResponse();
		}
		assertEquals(429, last.getStatus(), "Un X-Forwarded-For falso desde una IP no confiable no cambia el cupo");
	}

	@Test
	void twoUsersFromTheSameAddressHaveSeparateBuckets() throws Exception {
		MockMvc mvc = mvc(context);
		String ip = "10.253.0.1";
		UUID alice = UUID.randomUUID();
		UUID bob = UUID.randomUUID();
		int[] first = statuses(mvc, LIMIT + 1, () -> request(CATALOG).with(remoteAddr(ip))
				.with(authentication(authenticationFor(alice, Role.CLIENT))));
		int[] second = statuses(mvc, 1, () -> request(CATALOG).with(remoteAddr(ip))
				.with(authentication(authenticationFor(bob, Role.CLIENT))));
		assertEquals(429, first[LIMIT], "El tercer intento del primer usuario debe dar 429");
		assertEquals(200, second[0], "Otro usuario desde la misma IP tiene su propio cupo");
	}
}
