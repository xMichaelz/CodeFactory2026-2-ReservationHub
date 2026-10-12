package co.udea.codefactory.reservahub.reservation;

import co.udea.codefactory.reservahub.AbstractIntegrationTest;
import co.udea.codefactory.reservahub.TestApiClient;
import co.udea.codefactory.reservahub.reservation.entity.Reservation;
import co.udea.codefactory.reservahub.reservation.entity.ReservationStatus;
import co.udea.codefactory.reservahub.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AvailabilityControllerTest extends AbstractIntegrationTest {

	private static final JsonMapper JSON = JsonMapper.shared();

	@Autowired
	private WebApplicationContext context;
	@Autowired
	private ReservationRepository reservationRepository;
	@Value("${app.business.zone:America/Bogota}")
	private String zone;

	private MockMvc mvc() {
		return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
	}

	private LocalDate tomorrow() {
		return LocalDate.now(ZoneId.of(zone)).plusDays(1);
	}

	private record Client(String token, UUID id) {
	}

	private Client registerClient(MockMvc mvc) throws Exception {
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

	/** Crea servicio de 60 min con ventana 09:00–11:00 mañana; devuelve [serviceId, providerId]. */
	private String[] providerWithService(MockMvc mvc, String token) throws Exception {
		String service = mvc.perform(post("/api/v1/services").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Servicio " + UUID.randomUUID() + "\",\"durationMinutes\":60,\"price\":1000}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		mvc.perform(post("/api/v1/schedules").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"dayOfWeek\":\"%s\",\"startTime\":\"09:00\",\"endTime\":\"11:00\"}"
						.formatted(tomorrow().getDayOfWeek().name())))
				.andExpect(status().isCreated());
		JsonNode node = JSON.readTree(service);
		return new String[] { node.get("id").asText(), node.get("providerId").asText() };
	}

	private org.springframework.test.web.servlet.ResultActions query(MockMvc mvc, String token, String serviceId,
			LocalDate from, LocalDate to) throws Exception {
		var request = get("/api/v1/services/" + serviceId + "/availability?from=" + from + "&to=" + to);
		if (token != null) {
			request = request.header("Authorization", "Bearer " + token);
		}
		return mvc.perform(request);
	}

	@Test
	void clientAndProviderGetSlotsGroupedByDate() throws Exception {
		MockMvc mvc = mvc();
		String provider = TestApiClient.registerProviderAndLogin(mvc);
		String[] ids = providerWithService(mvc, provider);
		Client client = registerClient(mvc);
		for (String token : new String[] { client.token(), provider }) {
			String body = query(mvc, token, ids[0], tomorrow(), tomorrow().plusDays(1)).andExpect(status().isOk())
					.andReturn().getResponse().getContentAsString();
			JsonNode json = JSON.readTree(body);
			assertEquals(ids[0], json.get("serviceId").asText());
			JsonNode day = json.get("days").get(0);
			assertEquals(tomorrow().toString(), day.get("date").asText());
			assertEquals(2, day.get("slots").size());
			assertEquals(LocalTime.of(9, 0), LocalTime.parse(day.get("slots").get(0).get("start").asText()));
		}
	}

	@Test
	void bookedSlotDoesNotAppear() throws Exception {
		MockMvc mvc = mvc();
		String provider = TestApiClient.registerProviderAndLogin(mvc);
		String[] ids = providerWithService(mvc, provider);
		Client client = registerClient(mvc);
		Reservation reservation = new Reservation();
		reservation.setClientId(client.id());
		reservation.setCreatedBy(client.id());
		reservation.setServiceId(UUID.fromString(ids[0]));
		reservation.setProviderId(UUID.fromString(ids[1]));
		reservation.setReservationDate(tomorrow());
		reservation.setStartTime(LocalTime.of(9, 0));
		reservation.setEndTime(LocalTime.of(10, 0));
		reservation.setStatus(ReservationStatus.CONFIRMED);
		reservationRepository.save(reservation);
		String body = query(mvc, client.token(), ids[0], tomorrow(), tomorrow()).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		JsonNode slots = JSON.readTree(body).get("days").get(0).get("slots");
		assertEquals(1, slots.size());
		assertEquals(LocalTime.of(10, 0), LocalTime.parse(slots.get(0).get("start").asText()));
	}

	@Test
	void anonymousGets401() throws Exception {
		query(mvc(), null, UUID.randomUUID().toString(), tomorrow(), tomorrow()).andExpect(status().isUnauthorized());
	}

	@Test
	void unknownServiceGets404() throws Exception {
		MockMvc mvc = mvc();
		Client client = registerClient(mvc);
		query(mvc, client.token(), UUID.randomUUID().toString(), tomorrow(), tomorrow())
				.andExpect(status().isNotFound());
	}

	@Test
	void invalidRangesGet400() throws Exception {
		MockMvc mvc = mvc();
		String provider = TestApiClient.registerProviderAndLogin(mvc);
		String[] ids = providerWithService(mvc, provider);
		Client client = registerClient(mvc);
		query(mvc, client.token(), ids[0], tomorrow().plusDays(2), tomorrow()).andExpect(status().isBadRequest());
		query(mvc, client.token(), ids[0], tomorrow(), tomorrow().plusDays(31)).andExpect(status().isBadRequest());
		query(mvc, client.token(), ids[0], tomorrow(), tomorrow().plusDays(30)).andExpect(status().isOk());
	}
}
