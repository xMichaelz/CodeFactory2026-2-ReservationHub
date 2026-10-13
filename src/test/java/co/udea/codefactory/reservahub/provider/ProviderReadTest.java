package co.udea.codefactory.reservahub.provider;

import co.udea.codefactory.reservahub.AbstractIntegrationTest;
import co.udea.codefactory.reservahub.TestApiClient;
import co.udea.codefactory.reservahub.provider.repository.ProviderRepository;
import co.udea.codefactory.reservahub.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Lecturas del proveedor: sus servicios (con recursos), recursos y horarios (con bloqueos) y búsqueda de cliente. */
@SpringBootTest
class ProviderReadTest extends AbstractIntegrationTest {

	private static final JsonMapper JSON = JsonMapper.shared();

	@Autowired
	private WebApplicationContext context;
	@Autowired
	private ProviderRepository providerRepository;
	@Autowired
	private UserRepository userRepository;

	private MockMvc mvc() {
		return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
	}

	private JsonNode getJson(MockMvc mvc, String url, String token) throws Exception {
		String body = mvc.perform(get(url).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return JSON.readTree(body);
	}

	private String postJson(MockMvc mvc, String url, String token, String json) throws Exception {
		return JSON.readTree(mvc.perform(post(url).header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString())
				.get("id").asText();
	}

	private static List<String> ids(JsonNode array) {
		List<String> ids = new ArrayList<>();
		array.forEach(n -> ids.add(n.get("id").asText()));
		return ids;
	}

	@Test
	void servicesMineListsOnlyOwnServicesWithTheirResources() throws Exception {
		MockMvc mvc = mvc();
		String token = TestApiClient.registerProviderAndLogin(mvc);
		String other = TestApiClient.registerProviderAndLogin(mvc);
		String resource = postJson(mvc, "/api/v1/resources", token,
				"{\"name\":\"Sala " + UUID.randomUUID() + "\",\"resourceType\":\"SALA\",\"quantity\":2}");
		String withRes = postJson(mvc, "/api/v1/services", token,
				"{\"name\":\"Con " + UUID.randomUUID() + "\",\"durationMinutes\":30,\"price\":1000}");
		String without = postJson(mvc, "/api/v1/services", token,
				"{\"name\":\"Sin " + UUID.randomUUID() + "\",\"durationMinutes\":45,\"price\":500}");
		String foreign = postJson(mvc, "/api/v1/services", other,
				"{\"name\":\"Ajeno " + UUID.randomUUID() + "\",\"durationMinutes\":30,\"price\":1000}");
		mvc.perform(post("/api/v1/services/" + withRes + "/resources").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content("{\"resourceIds\":[\"" + resource + "\"]}"))
				.andExpect(status().isCreated());

		JsonNode list = getJson(mvc, "/api/v1/services/mine", token);

		assertEquals(2, list.size());
		assertTrue(ids(list).containsAll(List.of(withRes, without)));
		assertTrue(!ids(list).contains(foreign));
		for (JsonNode s : list) {
			if (s.get("id").asText().equals(withRes)) {
				assertEquals(1, s.get("resources").size());
				assertEquals(resource, s.get("resources").get(0).get("id").asText());
			} else {
				assertEquals(0, s.get("resources").size());
			}
			assertEquals("ACTIVE", s.get("status").asText());
		}
	}

	@Test
	void resourcesMineListsOnlyOwnResources() throws Exception {
		MockMvc mvc = mvc();
		String token = TestApiClient.registerProviderAndLogin(mvc);
		String other = TestApiClient.registerProviderAndLogin(mvc);
		String mine = postJson(mvc, "/api/v1/resources", token,
				"{\"name\":\"Sala " + UUID.randomUUID() + "\",\"resourceType\":\"SALA\",\"quantity\":2}");
		String foreign = postJson(mvc, "/api/v1/resources", other,
				"{\"name\":\"Sala " + UUID.randomUUID() + "\",\"resourceType\":\"SALA\",\"quantity\":1}");

		JsonNode list = getJson(mvc, "/api/v1/resources/mine", token);

		assertEquals(List.of(mine), ids(list));
		assertEquals(2, list.get(0).get("quantity").asInt());
		assertTrue(!ids(list).contains(foreign));
	}

	@Test
	void schedulesMineListsOwnWindowsWithBlocks() throws Exception {
		MockMvc mvc = mvc();
		String token = TestApiClient.registerProviderAndLogin(mvc);
		String other = TestApiClient.registerProviderAndLogin(mvc);
		String schedule = postJson(mvc, "/api/v1/schedules", token,
				"{\"dayOfWeek\":\"MONDAY\",\"startTime\":\"09:00\",\"endTime\":\"12:00\"}");
		postJson(mvc, "/api/v1/schedules", other,
				"{\"dayOfWeek\":\"MONDAY\",\"startTime\":\"09:00\",\"endTime\":\"12:00\"}");
		java.time.LocalDate monday = java.time.LocalDate.now().plusYears(1)
				.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY));
		String block = postJson(mvc, "/api/v1/schedules/" + schedule + "/blocks", token,
				"{\"blockDate\":\"%s\",\"startTime\":\"10:00\",\"endTime\":\"11:00\",\"reason\":\"x\"}"
						.formatted(monday));

		JsonNode list = getJson(mvc, "/api/v1/schedules/mine", token);

		assertEquals(List.of(schedule), ids(list));
		assertEquals("MONDAY", list.get(0).get("dayOfWeek").asText());
		assertEquals(List.of(block), ids(list.get(0).get("blocks")));
		assertEquals(monday.toString(), list.get(0).get("blocks").get(0).get("blockDate").asText());
	}

	@Test
	void lookupFindsActiveClientByEmailIgnoringCase() throws Exception {
		MockMvc mvc = mvc();
		String token = TestApiClient.registerProviderAndLogin(mvc);
		String email = "look-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
		String created = mvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(
				"{\"email\":\"%s\",\"password\":\"Secret123\",\"firstName\":\"Lola\",\"lastName\":\"Cli\"}"
						.formatted(email))).andExpect(status().isCreated()).andReturn().getResponse()
				.getContentAsString();
		String id = JSON.readTree(created).get("id").asText();

		JsonNode found = getJson(mvc, "/api/v1/users/lookup?email=" + email.toUpperCase(), token);

		assertEquals(id, found.get("id").asText());
		assertTrue(found.get("name").asText().contains("Lola"));
		assertTrue(!found.has("email") && !found.has("passwordHash"), "no debe exponer más datos");
	}

	@Test
	void lookupOfUnknownOrNonClientEmailIs404() throws Exception {
		MockMvc mvc = mvc();
		String token = TestApiClient.registerProviderAndLogin(mvc);
		String providerEmail = userRepository.findAll().stream()
				.filter(u -> u.getEmail().startsWith("prov-")).findFirst().orElseThrow().getEmail();
		mvc.perform(get("/api/v1/users/lookup?email=" + providerEmail).header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound());
		mvc.perform(get("/api/v1/users/lookup?email=nadie-" + UUID.randomUUID() + "@example.com")
				.header("Authorization", "Bearer " + token)).andExpect(status().isNotFound());
	}
}
