package co.udea.codefactory.reservahub;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Registra un proveedor y devuelve su token, para pruebas de integración. */
public final class TestApiClient {

	private static final JsonMapper JSON = JsonMapper.shared();

	private TestApiClient() {
	}

	public static String registerProviderAndLogin(MockMvc mvc) throws Exception {
		String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
		String email = "prov-" + suffix + "@example.com";
		mvc.perform(post("/api/v1/providers").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","password":"Secret123","firstName":"Ana","lastName":"Prov",
						 "businessName":"Negocio %s","serviceType":"Salud"}
						""".formatted(email, suffix)));
		String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"%s\",\"password\":\"Secret123\"}".formatted(email)))
				.andReturn().getResponse().getContentAsString();
		return JSON.readTree(body).get("accessToken").asText();
	}
}
