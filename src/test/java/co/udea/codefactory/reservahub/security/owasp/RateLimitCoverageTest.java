package co.udea.codefactory.reservahub.security.owasp;

import co.udea.codefactory.reservahub.AbstractIntegrationTest;
import co.udea.codefactory.reservahub.security.jwt.JwtService;
import co.udea.codefactory.reservahub.shared.enums.Role;
import org.junit.jupiter.api.Disabled;
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
		"app.rate-limit.lookup-per-minute=2"
})
class RateLimitCoverageTest extends AbstractIntegrationTest {

	private static final int LIMIT = 2;
	private static final Set<String> EXEMPT = Set.of("GET /actuator/health");
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
	@Disabled("pendiente V-03 · O4")
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
	@Disabled("pendiente V-03 · O4")
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
}
