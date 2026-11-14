package co.udea.codefactory.reservahub.security.owasp;

import co.udea.codefactory.reservahub.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static co.udea.codefactory.reservahub.security.owasp.OwaspTestSupport.discoverEndpoints;
import static co.udea.codefactory.reservahub.security.owasp.OwaspTestSupport.mvc;
import static co.udea.codefactory.reservahub.security.owasp.OwaspTestSupport.request;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * V-08 · Cabeceras de seguridad: toda respuesta de la API (endpoints descubiertos solos, como anónimo, más
 * {@code /actuator/health}) lleva las cabeceras del contrato. Una prueba por cabecera.
 */
@SpringBootTest
class ApiSecurityHeadersTest extends AbstractIntegrationTest {

	@Autowired
	private ApplicationContext context;

	private List<MockHttpServletRequestBuilder> requests(boolean secure) {
		List<MockHttpServletRequestBuilder> list = new ArrayList<>();
		for (String endpoint : discoverEndpoints(context)) {
			list.add(request(endpoint).secure(secure));
		}
		list.add(MockMvcRequestBuilders.get("/actuator/health").secure(secure));
		return list;
	}

	private void assertHeader(String name, Predicate<String> ok, String expected, boolean secure) throws Exception {
		MockMvc mvc = mvc(context);
		List<String> failures = new ArrayList<>();
		for (MockHttpServletRequestBuilder builder : requests(secure)) {
			MockHttpServletResponse r = mvc.perform(builder).andReturn().getResponse();
			String value = r.getHeader(name);
			if (value == null || !ok.test(value)) {
				var req = builder.buildRequest(context.getBean(jakarta.servlet.ServletContext.class));
				failures.add(req.getMethod() + " " + req.getRequestURI() + " → " + value);
			}
		}
		assertTrue(failures.isEmpty(), name + " debía ser «" + expected + "»: " + failures);
	}

	@Test
	void contentTypeOptionsNosniff() throws Exception {
		assertHeader("X-Content-Type-Options", "nosniff"::equals, "nosniff", false);
	}

	@Test
	void frameOptionsDeny() throws Exception {
		assertHeader("X-Frame-Options", "DENY"::equals, "DENY", false);
	}

	@Test
	void referrerPolicyNoReferrer() throws Exception {
		assertHeader("Referrer-Policy", "no-referrer"::equals, "no-referrer", false);
	}

	@Test
	void contentSecurityPolicyDenyAll() throws Exception {
		String csp = "default-src 'none'; frame-ancestors 'none'";
		assertHeader("Content-Security-Policy", csp::equals, csp, false);
	}

	@Test
	void cacheControlNoStore() throws Exception {
		assertHeader("Cache-Control", v -> v.contains("no-store"), "…no-store…", false);
	}

	@Test
	void strictTransportSecurityOnHttps() throws Exception {
		assertHeader("Strict-Transport-Security", v -> v.contains("max-age="), "max-age=…", true);
	}
}
