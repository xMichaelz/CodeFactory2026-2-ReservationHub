package co.udea.codefactory.reservahub;

import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class Sprint1AcceptanceTest extends AbstractIntegrationTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private JsonMapper jsonMapper;

	private MockMvc mockMvc;

	@Test
	void coversHu01ToHu10HappyPathAndKeyNegatives() throws Exception {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
				.apply(springSecurity())
				.build();

		String suffix = String.valueOf(System.currentTimeMillis());

		// HU-01 register client
		mockMvc.perform(post("/api/v1/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "client-%s@example.com",
								  "password": "Secret123",
								  "firstName": "Ana",
								  "lastName": "Cliente",
								  "phone": "3001112233"
								}
								""".formatted(suffix)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.role").value("CLIENT"))
				.andExpect(jsonPath("$.password").doesNotExist());

		// duplicate email
		mockMvc.perform(post("/api/v1/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "client-%s@example.com",
								  "password": "Secret123",
								  "firstName": "Ana",
								  "lastName": "Cliente"
								}
								""".formatted(suffix)))
				.andExpect(status().isConflict());

		// invalid email
		mockMvc.perform(post("/api/v1/users")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "not-an-email",
								  "password": "Secret123",
								  "firstName": "Ana",
								  "lastName": "Cliente"
								}
								"""))
				.andExpect(status().isBadRequest());

		// HU-02 register provider
		mockMvc.perform(post("/api/v1/providers")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "provider-%s@example.com",
								  "password": "Secret123",
								  "firstName": "Luis",
								  "lastName": "Proveedor",
								  "businessName": "Clinica %s",
								  "description": "Consultorio general",
								  "address": "Calle 1 # 2-3"
								}
								""".formatted(suffix, suffix)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id", notNullValue()));

		// HU-03 login
		MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "provider-%s@example.com",
								  "password": "Secret123"
								}
								""".formatted(suffix)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken", notNullValue()))
				.andReturn();

		String token = jsonMapper.readTree(login.getResponse().getContentAsString()).get("accessToken").asText();

		// bad credentials
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "provider-%s@example.com",
								  "password": "WrongPass1"
								}
								""".formatted(suffix)))
				.andExpect(status().isUnauthorized());

		// client cannot create services
		MvcResult clientLogin = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "client-%s@example.com",
								  "password": "Secret123"
								}
								""".formatted(suffix)))
				.andExpect(status().isOk())
				.andReturn();
		String clientToken = jsonMapper.readTree(clientLogin.getResponse().getContentAsString())
				.get("accessToken").asText();

		mockMvc.perform(post("/api/v1/services")
						.header("Authorization", "Bearer " + clientToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Consulta",
								  "description": "General",
								  "durationMinutes": 30,
								  "price": 50000
								}
								"""))
				.andExpect(status().isForbidden());

		// HU-05 create service
		MvcResult createdService = mockMvc.perform(post("/api/v1/services")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Consulta general",
								  "description": "30 minutos",
								  "durationMinutes": 30,
								  "price": 80000
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andReturn();
		String serviceId = jsonMapper.readTree(createdService.getResponse().getContentAsString()).get("id").asText();

		// duplicate service name
		mockMvc.perform(post("/api/v1/services")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Consulta general",
								  "durationMinutes": 30,
								  "price": 80000
								}
								"""))
				.andExpect(status().isConflict());

		// HU-06 update service
		mockMvc.perform(put("/api/v1/services/" + serviceId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Consulta prioritaria",
								  "description": "45 minutos",
								  "durationMinutes": 45,
								  "price": 120000
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Consulta prioritaria"));

		// HU-07 deactivate
		mockMvc.perform(patch("/api/v1/services/" + serviceId + "/deactivate")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("INACTIVE"));

		// HU-08 schedule
		MvcResult scheduleResult = mockMvc.perform(post("/api/v1/schedules")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "dayOfWeek": "MONDAY",
								  "startTime": "09:00:00",
								  "endTime": "12:00:00"
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String scheduleId = jsonMapper.readTree(scheduleResult.getResponse().getContentAsString()).get("id").asText();

		// invalid range
		mockMvc.perform(post("/api/v1/schedules")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "dayOfWeek": "TUESDAY",
								  "startTime": "12:00:00",
								  "endTime": "09:00:00"
								}
								"""))
				.andExpect(status().isBadRequest());

		// overlap
		mockMvc.perform(post("/api/v1/schedules")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "dayOfWeek": "MONDAY",
								  "startTime": "11:00:00",
								  "endTime": "13:00:00"
								}
								"""))
				.andExpect(status().isConflict());

		LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

		// HU-09 block
		mockMvc.perform(post("/api/v1/schedules/" + scheduleId + "/blocks")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "blockDate": "%s",
								  "startTime": "10:00:00",
								  "endTime": "11:00:00",
								  "reason": "Mantenimiento"
								}
								""".formatted(nextMonday)))
				.andExpect(status().isCreated());

		// HU-10 resource
		mockMvc.perform(post("/api/v1/resources")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Consultorio 1",
								  "resourceType": "ROOM",
								  "description": "Sala principal"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("ACTIVE"));

		// duplicate resource
		mockMvc.perform(post("/api/v1/resources")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Consultorio 1",
								  "resourceType": "ROOM"
								}
								"""))
				.andExpect(status().isConflict());

		// HU-04 logout + revoked token rejected
		mockMvc.perform(post("/api/v1/auth/logout")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/services")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name": "Otro servicio",
								  "durationMinutes": 20,
								  "price": 10000
								}
								"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void healthEndpointIsPublic() throws Exception {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
				.apply(springSecurity())
				.build();

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/actuator/health"))
				.andExpect(status().isOk());
	}
}
