package co.udea.codefactory.reservahub.reservation;

import co.udea.codefactory.reservahub.AbstractIntegrationTest;
import co.udea.codefactory.reservahub.TestApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Un parámetro de consulta ausente o mal escrito es un 400 {@code VALIDATION_ERROR}, no un 500. */
@SpringBootTest
class InvalidParamsTest extends AbstractIntegrationTest {

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;
	private String clientToken;
	private String providerToken;

	@BeforeEach
	void setUp() throws Exception {
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
		clientToken = ReservationTestSupport.registerClient(mvc).token();
		providerToken = TestApiClient.registerProviderAndLogin(mvc);
	}

	private void expectValidationError(String url, String token, String param) throws Exception {
		mvc.perform(get(url).header("Authorization", "Bearer " + token))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.details", hasItem(containsString(param))));
	}

	@Test
	void mineWithoutFromIs400() throws Exception {
		expectValidationError("/api/v1/reservations/mine?to=2026-10-20", clientToken, "from");
	}

	@Test
	void mineWithMalformedFromIs400() throws Exception {
		expectValidationError("/api/v1/reservations/mine?from=abc&to=2026-10-20", clientToken, "from");
	}

	@Test
	void availabilityWithoutToIs400() throws Exception {
		expectValidationError("/api/v1/services/" + UUID.randomUUID() + "/availability?from=2026-10-20",
				clientToken, "to");
	}

	@Test
	void availabilityWithMalformedServiceIdIs400() throws Exception {
		expectValidationError("/api/v1/services/no-es-uuid/availability?from=2026-10-20&to=2026-10-21",
				clientToken, "serviceId");
	}

	@Test
	void lookupWithoutEmailIs400() throws Exception {
		expectValidationError("/api/v1/users/lookup", providerToken, "email");
	}
}
