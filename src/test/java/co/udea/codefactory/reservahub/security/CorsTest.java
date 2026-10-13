package co.udea.codefactory.reservahub.security;

import co.udea.codefactory.reservahub.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CORS: el front de desarrollo (Vite) puede llamar a la API; otros orígenes no. */
@SpringBootTest
class CorsTest extends AbstractIntegrationTest {

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc() {
		return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
	}

	@Test
	void allowedOriginGetsCorsHeadersOnPreflight() throws Exception {
		mvc().perform(options("/api/v1/reservations/mine")
				.header("Origin", "http://localhost:5173")
				.header("Access-Control-Request-Method", "GET")
				.header("Access-Control-Request-Headers", "authorization,content-type"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
				.andExpect(header().exists("Access-Control-Allow-Methods"))
				.andExpect(header().exists("Access-Control-Allow-Headers"));
	}

	@Test
	void patchIsAllowedForTheReservationActions() throws Exception {
		mvc().perform(options("/api/v1/reservations/00000000-0000-0000-0000-000000000000/cancel")
				.header("Origin", "http://localhost:5173")
				.header("Access-Control-Request-Method", "PATCH"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
	}

	@Test
	void notAllowedOriginGetsNoCorsHeaders() throws Exception {
		mvc().perform(options("/api/v1/reservations/mine")
				.header("Origin", "http://evil.example.com")
				.header("Access-Control-Request-Method", "GET"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
	}
}
