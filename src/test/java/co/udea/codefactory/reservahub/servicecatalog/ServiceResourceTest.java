package co.udea.codefactory.reservahub.servicecatalog;

import co.udea.codefactory.reservahub.AbstractIntegrationTest;
import co.udea.codefactory.reservahub.TestApiClient;
import co.udea.codefactory.reservahub.resource.entity.Resource;
import co.udea.codefactory.reservahub.resource.repository.ResourceRepository;
import co.udea.codefactory.reservahub.shared.enums.EntityStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ServiceResourceTest extends AbstractIntegrationTest {

	private static final JsonMapper JSON = JsonMapper.shared();

	@Autowired
	private WebApplicationContext context;
	@Autowired
	private ResourceRepository resourceRepository;

	private MockMvc mvc() {
		return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
	}

	private String createService(MockMvc mvc, String token) throws Exception {
		String body = mvc.perform(post("/api/v1/services").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Servicio " + UUID.randomUUID() + "\",\"durationMinutes\":30,\"price\":1000}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return JSON.readTree(body).get("id").asText();
	}

	private String createResource(MockMvc mvc, String token) throws Exception {
		String body = mvc.perform(post("/api/v1/resources").header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Sala " + UUID.randomUUID() + "\",\"resourceType\":\"SALA\",\"quantity\":2}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return JSON.readTree(body).get("id").asText();
	}

	private org.springframework.test.web.servlet.ResultActions link(MockMvc mvc, String token, String serviceId,
			String... resourceIds) throws Exception {
		String ids = String.join("\",\"", resourceIds);
		return mvc.perform(post("/api/v1/services/" + serviceId + "/resources")
				.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
				.content("{\"resourceIds\":[\"" + ids + "\"]}"));
	}

	@Test
	void ownerLinksResourcesReturns201() throws Exception {
		MockMvc mvc = mvc();
		String token = TestApiClient.registerProviderAndLogin(mvc);
		link(mvc, token, createService(mvc, token), createResource(mvc, token), createResource(mvc, token))
				.andExpect(status().isCreated());
	}

	@Test
	void serviceOfAnotherProviderReturns403() throws Exception {
		MockMvc mvc = mvc();
		String owner = TestApiClient.registerProviderAndLogin(mvc);
		String other = TestApiClient.registerProviderAndLogin(mvc);
		link(mvc, other, createService(mvc, owner), createResource(mvc, other)).andExpect(status().isForbidden());
	}

	@Test
	void resourceOfAnotherProviderReturns403() throws Exception {
		MockMvc mvc = mvc();
		String owner = TestApiClient.registerProviderAndLogin(mvc);
		String other = TestApiClient.registerProviderAndLogin(mvc);
		link(mvc, owner, createService(mvc, owner), createResource(mvc, other)).andExpect(status().isForbidden());
	}

	@Test
	void inactiveResourceReturns400() throws Exception {
		MockMvc mvc = mvc();
		String token = TestApiClient.registerProviderAndLogin(mvc);
		String resourceId = createResource(mvc, token);
		Resource resource = resourceRepository.findById(UUID.fromString(resourceId)).orElseThrow();
		resource.setStatus(EntityStatus.INACTIVE);
		resourceRepository.save(resource);
		link(mvc, token, createService(mvc, token), resourceId).andExpect(status().isBadRequest());
	}

	@Test
	void repeatedAssociationReturns409() throws Exception {
		MockMvc mvc = mvc();
		String token = TestApiClient.registerProviderAndLogin(mvc);
		String serviceId = createService(mvc, token);
		String resourceId = createResource(mvc, token);
		link(mvc, token, serviceId, resourceId).andExpect(status().isCreated());
		link(mvc, token, serviceId, resourceId).andExpect(status().isConflict());
	}

	@Test
	void clientReturns403() throws Exception {
		MockMvc mvc = mvc();
		String email = "cli-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
		mvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(
				"{\"email\":\"%s\",\"password\":\"Secret123\",\"firstName\":\"A\",\"lastName\":\"C\"}".formatted(email)))
				.andExpect(status().isCreated());
		String login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\",\"password\":\"Secret123\"}".formatted(email)))
				.andReturn().getResponse().getContentAsString();
		String token = JSON.readTree(login).get("accessToken").asText();
		link(mvc, token, UUID.randomUUID().toString(), UUID.randomUUID().toString()).andExpect(status().isForbidden());
	}
}
