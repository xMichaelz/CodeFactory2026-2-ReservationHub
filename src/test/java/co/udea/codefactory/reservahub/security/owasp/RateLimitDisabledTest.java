package co.udea.codefactory.reservahub.security.owasp;

import co.udea.codefactory.reservahub.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static co.udea.codefactory.reservahub.security.owasp.OwaspTestSupport.mvc;
import static co.udea.codefactory.reservahub.security.owasp.OwaspTestSupport.remoteAddr;
import static co.udea.codefactory.reservahub.security.owasp.OwaspTestSupport.request;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** V-03 · {@code app.rate-limit.enabled=false} apaga el límite aunque los cupos sean mínimos. */
@SpringBootTest
@TestPropertySource(properties = {
		"app.rate-limit.enabled=false",
		"app.rate-limit.login-per-minute=1"
})
class RateLimitDisabledTest extends AbstractIntegrationTest {

	@Autowired
	private ApplicationContext context;

	@Test
	void disabledRateLimitNeverAnswers429() throws Exception {
		MockMvc mvc = mvc(context);
		for (int i = 0; i < 5; i++) {
			int status = mvc.perform(request("POST /api/v1/auth/login").with(remoteAddr("10.254.0.1")))
					.andReturn().getResponse().getStatus();
			assertNotEquals(429, status, "Con el límite apagado no debe haber 429 (intento " + (i + 1) + ")");
		}
	}
}
