package co.udea.codefactory.reservahub.security.owasp;

import co.udea.codefactory.reservahub.AbstractIntegrationTest;
import co.udea.codefactory.reservahub.security.jwt.JwtService;
import co.udea.codefactory.reservahub.shared.enums.Role;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static co.udea.codefactory.reservahub.security.owasp.OwaspTestSupport.authenticationFor;
import static co.udea.codefactory.reservahub.security.owasp.OwaspTestSupport.bearerFor;
import static co.udea.codefactory.reservahub.security.owasp.OwaspTestSupport.mvc;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** V-06 · Swagger/OpenAPI apagados por defecto; solo se publican si se encienden explícitamente (modo dev). */
@SpringBootTest
class SwaggerExposureTest extends AbstractIntegrationTest {

	static final List<String> PATHS = List.of("/v3/api-docs", "/swagger-ui.html", "/swagger-ui/index.html");

	@Autowired
	private ApplicationContext context;
	@Autowired
	private JwtService jwtService;

	@Test
	void swaggerIsNotPublishedByDefault() throws Exception {
		MockMvc mvc = mvc(context);
		UUID user = UUID.randomUUID();
		List<String> failures = new ArrayList<>();
		for (String path : PATHS) {
			int anonymous = OwaspTestSupport.status(mvc, get(path));
			int withToken = OwaspTestSupport.status(mvc, get(path)
					.header("Authorization", bearerFor(jwtService, user, Role.CLIENT))
					.with(authentication(authenticationFor(user, Role.CLIENT))));
			if (anonymous != 404) {
				failures.add(path + " anónimo → " + anonymous);
			}
			if (withToken != 404) {
				failures.add(path + " con token → " + withToken);
			}
		}
		assertTrue(failures.isEmpty(), "Swagger debía responder 404: " + failures);
	}

	@Nested
	@TestPropertySource(properties = {"springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=true"})
	class WhenExplicitlyEnabled {

		@Test
		void swaggerIsPublished() throws Exception {
			MockMvc mvc = mvc(context);
			List<String> failures = new ArrayList<>();
			for (String path : List.of("/v3/api-docs", "/swagger-ui/index.html")) {
				int status = OwaspTestSupport.status(mvc, get(path));
				if (status != 200) {
					failures.add(path + " → " + status);
				}
			}
			int html = OwaspTestSupport.status(mvc, get("/swagger-ui.html"));
			if (html != 200 && html != 302) {
				failures.add("/swagger-ui.html → " + html);
			}
			assertTrue(failures.isEmpty(), "Con springdoc encendido Swagger debía publicarse: " + failures);
		}
	}
}
