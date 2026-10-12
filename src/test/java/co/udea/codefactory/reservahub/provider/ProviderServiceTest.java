package co.udea.codefactory.reservahub.provider;

import co.udea.codefactory.reservahub.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ProviderServiceTest extends AbstractIntegrationTest {

	@Autowired
	private WebApplicationContext context;
	@Autowired
	private JdbcTemplate jdbc;

	private MockMvc mvc() {
		return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
	}

	private static String body(String suffix, String serviceTypeField) {
		return """
				{"email":"p-%s@example.com","password":"Secret123","firstName":"Ana","lastName":"Prov",
				 "businessName":"Negocio %s"%s}
				""".formatted(suffix, suffix, serviceTypeField);
	}

	@Test
	void serviceTypeIsPersistedAndReturned() throws Exception {
		String suffix = "st" + System.nanoTime();
		mvc().perform(post("/api/v1/providers").contentType(MediaType.APPLICATION_JSON)
						.content(body(suffix, ",\"serviceType\":\"Salud\"")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.serviceType").value("Salud"));
		String stored = jdbc.queryForObject("SELECT service_type FROM providers WHERE business_name = ?",
				String.class, "Negocio " + suffix);
		assertEquals("Salud", stored);
	}

	@Test
	void serviceTypeIsRequired() throws Exception {
		mvc().perform(post("/api/v1/providers").contentType(MediaType.APPLICATION_JSON)
						.content(body("nost" + System.nanoTime(), "")))
				.andExpect(status().isBadRequest());
	}
}
