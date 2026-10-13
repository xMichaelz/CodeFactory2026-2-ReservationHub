package co.udea.codefactory.reservahub.resource;

import co.udea.codefactory.reservahub.AbstractIntegrationTest;
import co.udea.codefactory.reservahub.TestApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ResourceServiceTest extends AbstractIntegrationTest {

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc() {
		return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
	}

	private static MockHttpServletRequestBuilder create(String token, String name, String quantityField) {
		return post("/api/v1/resources").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"%s\",\"resourceType\":\"ROOM\"%s}".formatted(name, quantityField));
	}

	@Test
	void quantityIsPersistedAndReturned() throws Exception {
		MockMvc mvc = mvc();
		String token = TestApiClient.registerProviderAndLogin(mvc);
		mvc.perform(create(token, "Sala", ",\"quantity\":3"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.quantity").value(3));
	}

	@Test
	void quantityIsRequiredAndAtLeastOne() throws Exception {
		MockMvc mvc = mvc();
		String token = TestApiClient.registerProviderAndLogin(mvc);
		mvc.perform(create(token, "SinCantidad", "")).andExpect(status().isBadRequest());
		mvc.perform(create(token, "Cero", ",\"quantity\":0")).andExpect(status().isBadRequest());
	}
}
