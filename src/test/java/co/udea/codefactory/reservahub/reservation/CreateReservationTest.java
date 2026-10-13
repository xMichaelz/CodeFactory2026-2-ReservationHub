package co.udea.codefactory.reservahub.reservation;

import co.udea.codefactory.reservahub.AbstractIntegrationTest;
import co.udea.codefactory.reservahub.reservation.ReservationTestSupport.Client;
import co.udea.codefactory.reservahub.reservation.ReservationTestSupport.ProviderSetup;
import co.udea.codefactory.reservahub.reservation.entity.ReservationAction;
import co.udea.codefactory.reservahub.reservation.entity.ReservationHistory;
import co.udea.codefactory.reservahub.reservation.repository.ReservationHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static co.udea.codefactory.reservahub.reservation.ReservationTestSupport.JSON;
import static co.udea.codefactory.reservahub.reservation.ReservationTestSupport.block;
import static co.udea.codefactory.reservahub.reservation.ReservationTestSupport.book;
import static co.udea.codefactory.reservahub.reservation.ReservationTestSupport.deactivate;
import static co.udea.codefactory.reservahub.reservation.ReservationTestSupport.providerWithService;
import static co.udea.codefactory.reservahub.reservation.ReservationTestSupport.registerClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-12: el cliente reserva un horario libre; nunca un horario no ofrecido ni cruzado con otra reserva suya. */
@SpringBootTest
class CreateReservationTest extends AbstractIntegrationTest {

	private static final LocalTime NINE = LocalTime.of(9, 0);
	private static final LocalTime TEN = LocalTime.of(10, 0);
	private static final LocalTime ELEVEN = LocalTime.of(11, 0);

	@Autowired
	private WebApplicationContext context;
	@Autowired
	private ReservationHistoryRepository historyRepository;
	@Value("${app.business.zone:America/Bogota}")
	private String zone;

	private MockMvc mvc() {
		return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
	}

	private LocalDate tomorrow() {
		return ReservationTestSupport.tomorrow(zone);
	}

	@Test
	void clientBooksFreeSlotAndGetsConfirmation() throws Exception {
		MockMvc mvc = mvc();
		ProviderSetup provider = providerWithService(mvc, tomorrow(), NINE, ELEVEN);
		Client client = registerClient(mvc);

		String body = book(mvc, client.token(), provider.serviceId(), tomorrow(), NINE)
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

		JsonNode json = JSON.readTree(body);
		assertEquals(provider.serviceId(), json.get("serviceId").asText());
		assertEquals(provider.providerId(), json.get("providerId").asText());
		assertEquals(client.id().toString(), json.get("clientId").asText());
		assertEquals(client.id().toString(), json.get("createdBy").asText());
		assertEquals(tomorrow().toString(), json.get("date").asText());
		assertEquals(NINE, LocalTime.parse(json.get("startTime").asText()));
		assertEquals(TEN, LocalTime.parse(json.get("endTime").asText()));
		assertEquals("CONFIRMED", json.get("status").asText());
		assertEquals(true, json.get("serviceName").asText().startsWith("Servicio "));
		assertEquals(true, json.get("providerName").asText().startsWith("Negocio "));

		List<ReservationHistory> history = historyRepository
				.findByReservationIdOrderByCreatedAtAsc(UUID.fromString(json.get("id").asText()));
		assertEquals(1, history.size());
		assertEquals(ReservationAction.CREATED, history.get(0).getAction());
		assertEquals(client.id(), history.get(0).getActorUserId());

		String availability = mvc.perform(get("/api/v1/services/" + provider.serviceId() + "/availability?from="
				+ tomorrow() + "&to=" + tomorrow()).header("Authorization", "Bearer " + client.token()))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		JsonNode slots = JSON.readTree(availability).get("days").get(0).get("slots");
		assertEquals(1, slots.size());
		assertEquals(TEN, LocalTime.parse(slots.get(0).get("start").asText()));
	}

	@Test
	void slotOutsideTheScheduleOrOffTheGridGets409() throws Exception {
		MockMvc mvc = mvc();
		ProviderSetup provider = providerWithService(mvc, tomorrow(), NINE, ELEVEN);
		Client client = registerClient(mvc);
		book(mvc, client.token(), provider.serviceId(), tomorrow(), LocalTime.of(12, 0))
				.andExpect(status().isConflict());
		book(mvc, client.token(), provider.serviceId(), tomorrow(), LocalTime.of(10, 30))
				.andExpect(status().isConflict());
		book(mvc, client.token(), provider.serviceId(), tomorrow().plusDays(1), NINE)
				.andExpect(status().isConflict());
	}

	@Test
	void blockedSlotGets409() throws Exception {
		MockMvc mvc = mvc();
		ProviderSetup provider = providerWithService(mvc, tomorrow(), NINE, ELEVEN);
		block(mvc, provider, tomorrow(), TEN, ELEVEN);
		Client client = registerClient(mvc);
		book(mvc, client.token(), provider.serviceId(), tomorrow(), TEN).andExpect(status().isConflict());
	}

	@Test
	void slotAlreadyTakenByAnotherClientGets409() throws Exception {
		MockMvc mvc = mvc();
		ProviderSetup provider = providerWithService(mvc, tomorrow(), NINE, ELEVEN);
		Client first = registerClient(mvc);
		Client second = registerClient(mvc);
		book(mvc, first.token(), provider.serviceId(), tomorrow(), NINE).andExpect(status().isCreated());
		book(mvc, second.token(), provider.serviceId(), tomorrow(), NINE).andExpect(status().isConflict());
	}

	@Test
	void pastDateGets400() throws Exception {
		MockMvc mvc = mvc();
		LocalDate yesterday = tomorrow().minusDays(2);
		ProviderSetup provider = providerWithService(mvc, yesterday, NINE, ELEVEN);
		Client client = registerClient(mvc);
		book(mvc, client.token(), provider.serviceId(), yesterday, NINE).andExpect(status().isBadRequest());
	}

	@Test
	void inactiveServiceGets400() throws Exception {
		MockMvc mvc = mvc();
		ProviderSetup provider = providerWithService(mvc, tomorrow(), NINE, ELEVEN);
		deactivate(mvc, provider);
		Client client = registerClient(mvc);
		book(mvc, client.token(), provider.serviceId(), tomorrow(), NINE).andExpect(status().isBadRequest());
	}

	@Test
	void clientWithAnOverlappingReservationGets409() throws Exception {
		MockMvc mvc = mvc();
		ProviderSetup one = providerWithService(mvc, tomorrow(), NINE, ELEVEN);
		ProviderSetup other = providerWithService(mvc, tomorrow(), NINE, ELEVEN);
		Client client = registerClient(mvc);
		book(mvc, client.token(), one.serviceId(), tomorrow(), NINE).andExpect(status().isCreated());
		book(mvc, client.token(), other.serviceId(), tomorrow(), NINE).andExpect(status().isConflict());
		book(mvc, client.token(), other.serviceId(), tomorrow(), TEN).andExpect(status().isCreated());
	}

	@Test
	void providerWithoutClientIdGets400() throws Exception {
		MockMvc mvc = mvc();
		ProviderSetup provider = providerWithService(mvc, tomorrow(), NINE, ELEVEN);
		book(mvc, provider.token(), provider.serviceId(), tomorrow(), NINE).andExpect(status().isBadRequest());
	}

	@Test
	void unknownServiceGets404AndMissingFieldsGet400() throws Exception {
		MockMvc mvc = mvc();
		Client client = registerClient(mvc);
		book(mvc, client.token(), UUID.randomUUID().toString(), tomorrow(), NINE).andExpect(status().isNotFound());
		mvc.perform(post("/api/v1/reservations").header("Authorization", "Bearer " + client.token())
				.contentType(MediaType.APPLICATION_JSON).content("{\"date\":\"" + tomorrow() + "\"}"))
				.andExpect(status().isBadRequest());
	}
}
