package co.udea.codefactory.reservahub.reservation;

import co.udea.codefactory.reservahub.TestApiClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Datos de prueba de reservas: proveedor con servicio y agenda, clientes y la llamada de creación. */
final class ReservationTestSupport {

	static final JsonMapper JSON = JsonMapper.shared();

	private ReservationTestSupport() {
	}

	record Client(String token, UUID id) {
	}

	/** Proveedor con un servicio de 60 min y una ventana de atención el día dado. */
	record ProviderSetup(String token, String serviceId, String providerId, String scheduleId) {
	}

	static LocalDate tomorrow(String zone) {
		return LocalDate.now(ZoneId.of(zone)).plusDays(1);
	}

	static Client registerClient(MockMvc mvc) throws Exception {
		String email = "cli-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
		String created = mvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(
				"{\"email\":\"%s\",\"password\":\"Secret123\",\"firstName\":\"A\",\"lastName\":\"C\"}".formatted(email)))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		String login = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\",\"password\":\"Secret123\"}".formatted(email)))
				.andReturn().getResponse().getContentAsString();
		return new Client(JSON.readTree(login).get("accessToken").asText(),
				UUID.fromString(JSON.readTree(created).get("id").asText()));
	}

	static ProviderSetup providerWithService(MockMvc mvc, LocalDate day, LocalTime start, LocalTime end)
			throws Exception {
		String token = TestApiClient.registerProviderAndLogin(mvc);
		String service = mvc.perform(post("/api/v1/services").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Servicio " + UUID.randomUUID() + "\",\"durationMinutes\":60,\"price\":1000}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		String schedule = mvc.perform(post("/api/v1/schedules").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"dayOfWeek\":\"%s\",\"startTime\":\"%s\",\"endTime\":\"%s\"}"
						.formatted(day.getDayOfWeek().name(), start, end)))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		JsonNode node = JSON.readTree(service);
		return new ProviderSetup(token, node.get("id").asText(), node.get("providerId").asText(),
				JSON.readTree(schedule).get("id").asText());
	}

	static void block(MockMvc mvc, ProviderSetup provider, LocalDate day, LocalTime start, LocalTime end)
			throws Exception {
		mvc.perform(post("/api/v1/schedules/" + provider.scheduleId() + "/blocks")
				.header("Authorization", "Bearer " + provider.token()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"blockDate\":\"%s\",\"startTime\":\"%s\",\"endTime\":\"%s\",\"reason\":\"x\"}"
						.formatted(day, start, end)))
				.andExpect(status().isCreated());
	}

	static void deactivate(MockMvc mvc, ProviderSetup provider) throws Exception {
		mvc.perform(patch("/api/v1/services/" + provider.serviceId() + "/deactivate")
				.header("Authorization", "Bearer " + provider.token())).andExpect(status().isOk());
	}

	static ResultActions book(MockMvc mvc, String token, String serviceId, LocalDate date, LocalTime start)
			throws Exception {
		return mvc.perform(post("/api/v1/reservations").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"serviceId\":\"%s\",\"date\":\"%s\",\"startTime\":\"%s\"}".formatted(serviceId, date,
						start)));
	}
}
