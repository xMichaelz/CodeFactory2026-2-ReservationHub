package co.udea.codefactory.reservahub.security;

import co.udea.codefactory.reservahub.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import co.udea.codefactory.reservahub.security.jwt.JwtService;
import co.udea.codefactory.reservahub.shared.enums.Role;
import co.udea.codefactory.reservahub.user.entity.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Matriz de seguridad: cada endpoint del backend debe tener una fila {método ruta → roles permitidos}.
 * Descubre los endpoints solo; un endpoint nuevo sin fila hace fallar la prueba.
 */
@SpringBootTest
class EndpointSecurityMatrixTest extends AbstractIntegrationTest {

	private static final String CLIENT = "CLIENT";
	private static final String PROVIDER = "PROVIDER";
	private static final String ADMIN = "ADMIN";
	private static final Set<String> ALL_ROLES = Set.of(CLIENT, PROVIDER, ADMIN);

	/** Endpoints públicos (anónimo permitido). */
	private static final Set<String> PUBLIC = Set.of(
			"POST /api/v1/users", "POST /api/v1/providers", "POST /api/v1/auth/login");

	/** Endpoints autenticados: clave "MÉTODO ruta" → roles permitidos. */
	private static final Map<String, Set<String>> MATRIX = Map.ofEntries(
			Map.entry("POST /api/v1/auth/logout", ALL_ROLES),
			Map.entry("POST /api/v1/services", Set.of(PROVIDER)),
			Map.entry("PUT /api/v1/services/{id}", Set.of(PROVIDER)),
			Map.entry("PATCH /api/v1/services/{id}/deactivate", Set.of(PROVIDER)),
			Map.entry("POST /api/v1/services/{id}/resources", Set.of(PROVIDER)),
			Map.entry("GET /api/v1/services/{serviceId}/availability", Set.of(CLIENT, PROVIDER)),
			Map.entry("POST /api/v1/schedules", Set.of(PROVIDER)),
			Map.entry("POST /api/v1/schedules/{id}/blocks", Set.of(PROVIDER)),
			Map.entry("POST /api/v1/resources", Set.of(PROVIDER)),
			Map.entry("POST /api/v1/reservations", Set.of(CLIENT, PROVIDER)),
			Map.entry("PATCH /api/v1/reservations/{id}/cancel", Set.of(CLIENT, PROVIDER)));

	@Autowired
	private ApplicationContext context;
	@Autowired
	private JwtService jwtService;

	private MockMvc mvc() {
		return MockMvcBuilders.webAppContextSetup((org.springframework.web.context.WebApplicationContext) context)
				.apply(springSecurity()).build();
	}

	private List<String> discoverEndpoints() {
		RequestMappingHandlerMapping mapping = context.getBean("requestMappingHandlerMapping",
				RequestMappingHandlerMapping.class);
		Set<String> found = new TreeSet<>();
		for (Map.Entry<RequestMappingInfo, HandlerMethod> e : mapping.getHandlerMethods().entrySet()) {
			if (!e.getValue().getBeanType().getName().startsWith("co.udea.codefactory")) {
				continue;
			}
			Set<String> paths = e.getKey().getPathPatternsCondition().getPatternValues();
			for (String path : paths) {
				for (var method : e.getKey().getMethodsCondition().getMethods()) {
					found.add(method.name() + " " + path);
				}
			}
		}
		return new ArrayList<>(found);
	}

	private String bearerFor(String role) {
		User u = new User();
		u.setId(UUID.randomUUID());
		u.setEmail("u@example.com");
		u.setRole(Role.valueOf(role));
		return "Bearer " + jwtService.generateToken(u);
	}

	private static Authentication authenticationFor(String role) {
		var principal = new SecurityUtils.AuthenticatedUser(UUID.randomUUID(), "u@example.com", Role.valueOf(role));
		return new UsernamePasswordAuthenticationToken(principal, null,
				List.of(new SimpleGrantedAuthority("ROLE_" + role)));
	}

	private static MockHttpServletRequestBuilder request(String key) {
		String[] parts = key.split(" ", 2);
		String path = parts[1].replaceAll("\\{[^}]+}", "00000000-0000-0000-0000-000000000000");
		return MockMvcRequestBuilders.request(HttpMethod.valueOf(parts[0]), path)
				.contentType(MediaType.APPLICATION_JSON).content("{}");
	}

	@Test
	void everyEndpointHasARowInTheMatrix() {
		List<String> missing = new ArrayList<>();
		for (String endpoint : discoverEndpoints()) {
			if (!PUBLIC.contains(endpoint) && !MATRIX.containsKey(endpoint)) {
				missing.add(endpoint);
			}
		}
		assertTrue(missing.isEmpty(), "Endpoints sin fila en la matriz de roles: " + missing);
	}

	@Test
	void matrixHasNoStaleRows() {
		List<String> discovered = discoverEndpoints();
		List<String> stale = new ArrayList<>();
		for (String key : MATRIX.keySet()) {
			if (!discovered.contains(key)) {
				stale.add(key);
			}
		}
		assertTrue(stale.isEmpty(), "Filas de la matriz sin endpoint real: " + stale);
	}

	@Test
	void anonymousGets401ExceptOnPublicEndpoints() throws Exception {
		MockMvc mvc = mvc();
		List<String> failures = new ArrayList<>();
		for (String endpoint : discoverEndpoints()) {
			if (PUBLIC.contains(endpoint)) {
				continue;
			}
			int status = mvc.perform(request(endpoint)).andReturn().getResponse().getStatus();
			if (status != 401) {
				failures.add(endpoint + " → " + status);
			}
		}
		assertTrue(failures.isEmpty(), "Anónimo debía recibir 401: " + failures);
	}

	@Test
	void allowedRolesPassAndOtherRolesGet403() throws Exception {
		MockMvc mvc = mvc();
		List<String> failures = new ArrayList<>();
		for (Map.Entry<String, Set<String>> row : MATRIX.entrySet()) {
			for (String role : ALL_ROLES) {
				int status;
				try {
					status = mvc.perform(request(row.getKey()).header("Authorization", bearerFor(role))
							.with(authentication(authenticationFor(role))))
							.andReturn().getResponse().getStatus();
				} catch (Exception ex) {
					status = 500; // pasó la seguridad y falló después (sin usuario real): no es 401/403
				}
				boolean allowed = row.getValue().contains(role);
				if (allowed && (status == 401 || status == 403)) {
					failures.add(row.getKey() + " con " + role + " debía pasar, dio " + status);
				}
				if (!allowed && status != 403) {
					failures.add(row.getKey() + " con " + role + " debía dar 403, dio " + status);
				}
			}
		}
		assertTrue(failures.isEmpty(), "Matriz de roles incumplida: " + failures);
	}
}
