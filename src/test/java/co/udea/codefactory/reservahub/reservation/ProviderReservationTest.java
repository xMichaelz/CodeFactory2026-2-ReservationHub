package co.udea.codefactory.reservahub.reservation;

import co.udea.codefactory.reservahub.AbstractIntegrationTest;
import co.udea.codefactory.reservahub.TestApiClient;
import co.udea.codefactory.reservahub.provider.repository.ProviderRepository;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static co.udea.codefactory.reservahub.reservation.ReservationTestSupport.JSON;
import static co.udea.codefactory.reservahub.reservation.ReservationTestSupport.providerWithService;
import static co.udea.codefactory.reservahub.reservation.ReservationTestSupport.registerClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-13: el proveedor reserva en un servicio propio a nombre de un cliente existente. */
@SpringBootTest
class ProviderReservationTest extends AbstractIntegrationTest {

	private static final LocalTime NINE = LocalTime.of(9, 0);
	private static final LocalTime ELEVEN = LocalTime.of(11, 0);

	@Autowired
	private WebApplicationContext context;
	@Autowired
	private ReservationHistoryRepository historyRepository;
	@Autowired
	private ProviderRepository providerRepository;
	@Value("${app.business.zone:America/Bogota}")
	private String zone;

	private MockMvc mvc() {
		return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
	}

	private LocalDate tomorrow() {
		return ReservationTestSupport.tomorrow(zone);
	}

	private ResultActions bookFor(MockMvc mvc, String token, String serviceId, Object clientId) throws Exception {
		String client = clientId == null ? "" : ",\"clientId\":\"%s\"".formatted(clientId);
		return mvc.perform(post("/api/v1/reservations").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content("{\"serviceId\":\"%s\",\"date\":\"%s\",\"startTime\":\"%s\"%s}"
						.formatted(serviceId, tomorrow(), NINE, client)));
	}

	@Test
	void providerBooksForClientAndIsRecordedAsCreator() throws Exception {
		MockMvc mvc = mvc();
		ProviderSetup provider = providerWithService(mvc, tomorrow(), NINE, ELEVEN);
		Client client = registerClient(mvc);

		String body = bookFor(mvc, provider.token(), provider.serviceId(), client.id())
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

		JsonNode json = JSON.readTree(body);
		assertEquals(client.id().toString(), json.get("clientId").asText());
		List<ReservationHistory> history = historyRepository
				.findAll().stream().filter(h -> h.getReservationId().toString().equals(json.get("id").asText()))
				.toList();
		assertEquals(1, history.size());
		assertEquals(ReservationAction.CREATED, history.get(0).getAction());
		assertEquals(json.get("createdBy").asText(), history.get(0).getActorUserId().toString());
		assertEquals(false, json.get("createdBy").asText().equals(client.id().toString()));
	}

	@Test
	void providerCannotBookServiceOfAnotherProvider() throws Exception {
		MockMvc mvc = mvc();
		ProviderSetup owner = providerWithService(mvc, tomorrow(), NINE, ELEVEN);
		String otherToken = TestApiClient.registerProviderAndLogin(mvc);
		Client client = registerClient(mvc);

		bookFor(mvc, otherToken, owner.serviceId(), client.id()).andExpect(status().isForbidden());
	}

	@Test
	void unknownClientIsNotFound() throws Exception {
		MockMvc mvc = mvc();
		ProviderSetup provider = providerWithService(mvc, tomorrow(), NINE, ELEVEN);

		bookFor(mvc, provider.token(), provider.serviceId(), UUID.randomUUID()).andExpect(status().isNotFound());
	}

	@Test
	void clientIdOfAProviderIsRejected() throws Exception {
		MockMvc mvc = mvc();
		ProviderSetup provider = providerWithService(mvc, tomorrow(), NINE, ELEVEN);
		ProviderSetup other = providerWithService(mvc, tomorrow(), NINE, ELEVEN);
		UUID otherUserId = providerRepository.findById(UUID.fromString(other.providerId())).orElseThrow().getUserId();

		bookFor(mvc, provider.token(), provider.serviceId(), otherUserId).andExpect(status().isBadRequest());
	}

	@Test
	void clientCannotBookOnBehalfOfAnotherClient() throws Exception {
		MockMvc mvc = mvc();
		ProviderSetup provider = providerWithService(mvc, tomorrow(), NINE, ELEVEN);
		Client actor = registerClient(mvc);
		Client other = registerClient(mvc);

		bookFor(mvc, actor.token(), provider.serviceId(), other.id()).andExpect(status().isForbidden());
	}
}
