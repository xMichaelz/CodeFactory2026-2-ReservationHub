package co.udea.codefactory.reservahub.reservation;

import co.udea.codefactory.reservahub.AbstractIntegrationTest;
import co.udea.codefactory.reservahub.TestApiClient;
import co.udea.codefactory.reservahub.reservation.ReservationTestSupport.Client;
import co.udea.codefactory.reservahub.reservation.ReservationTestSupport.ProviderSetup;
import co.udea.codefactory.reservahub.reservation.entity.Reservation;
import co.udea.codefactory.reservahub.reservation.entity.ReservationAction;
import co.udea.codefactory.reservahub.reservation.entity.ReservationHistory;
import co.udea.codefactory.reservahub.reservation.entity.ReservationStatus;
import co.udea.codefactory.reservahub.reservation.repository.ReservationHistoryRepository;
import co.udea.codefactory.reservahub.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static co.udea.codefactory.reservahub.reservation.ReservationTestSupport.JSON;
import static co.udea.codefactory.reservahub.reservation.ReservationTestSupport.book;
import static co.udea.codefactory.reservahub.reservation.ReservationTestSupport.providerWithService;
import static co.udea.codefactory.reservahub.reservation.ReservationTestSupport.registerClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-14 y HU-15: cancelación por el cliente y por el proveedor. */
@SpringBootTest
class CancelReservationTest extends AbstractIntegrationTest {

	private static final LocalTime NINE = LocalTime.of(9, 0);
	private static final LocalTime ELEVEN = LocalTime.of(11, 0);

	@Autowired
	private WebApplicationContext context;
	@Autowired
	private ReservationRepository reservationRepository;
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

	private String bookAs(MockMvc mvc, Client client, ProviderSetup provider) throws Exception {
		String body = book(mvc, client.token(), provider.serviceId(), tomorrow(), NINE).andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return JSON.readTree(body).get("id").asText();
	}

	private ResultActions cancel(MockMvc mvc, String token, String id, String reason) throws Exception {
		String body = reason == null ? "{}" : "{\"reason\":\"%s\"}".formatted(reason);
		return mvc.perform(patch("/api/v1/reservations/" + id + "/cancel").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	@Test
	void clientCancelsOwnFutureReservationAndSlotIsFreeAgain() throws Exception {
		MockMvc mvc = mvc();
		ProviderSetup provider = providerWithService(mvc, tomorrow(), NINE, ELEVEN);
		Client client = registerClient(mvc);
		String id = bookAs(mvc, client, provider);

		cancel(mvc, client.token(), id, null).andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED_BY_CLIENT"));

		mvc.perform(get("/api/v1/services/" + provider.serviceId() + "/availability?from=" + tomorrow() + "&to="
				+ tomorrow()).header("Authorization", "Bearer " + client.token())).andExpect(status().isOk())
				.andExpect(jsonPath("$.days[0].slots[0].start").value("09:00:00"));
		List<ReservationHistory> history = historyRepository.findAll().stream()
				.filter(h -> h.getReservationId().toString().equals(id)).toList();
		assertEquals(2, history.size());
		assertEquals(1, history.stream().filter(h -> h.getAction() == ReservationAction.CANCELLED).count());
		assertEquals(client.id(), history.stream().filter(h -> h.getAction() == ReservationAction.CANCELLED)
				.findFirst().orElseThrow().getActorUserId());
	}

	@Test
	void anotherClientCannotCancel() throws Exception {
		MockMvc mvc = mvc();
		ProviderSetup provider = providerWithService(mvc, tomorrow(), NINE, ELEVEN);
		String id = bookAs(mvc, registerClient(mvc), provider);

		cancel(mvc, registerClient(mvc).token(), id, null).andExpect(status().isForbidden());
	}

	@Test
	void pastReservationCannotBeCancelled() throws Exception {
		MockMvc mvc = mvc();
		ProviderSetup provider = providerWithService(mvc, tomorrow(), NINE, ELEVEN);
		Client client = registerClient(mvc);
		String id = bookAs(mvc, client, provider);
		Reservation reservation = reservationRepository.findById(java.util.UUID.fromString(id)).orElseThrow();
		reservation.setReservationDate(tomorrow().minusDays(3));
		reservationRepository.save(reservation);

		cancel(mvc, client.token(), id, null).andExpect(status().isBadRequest());
	}

	@Test
	void alreadyCancelledReturns409() throws Exception {
		MockMvc mvc = mvc();
		ProviderSetup provider = providerWithService(mvc, tomorrow(), NINE, ELEVEN);
		Client client = registerClient(mvc);
		String id = bookAs(mvc, client, provider);
		cancel(mvc, client.token(), id, null).andExpect(status().isOk());

		cancel(mvc, client.token(), id, null).andExpect(status().isConflict());
	}

	@Test
	void providerNeedsReasonAndCancelsOwnAgenda() throws Exception {
		MockMvc mvc = mvc();
		ProviderSetup provider = providerWithService(mvc, tomorrow(), NINE, ELEVEN);
		String id = bookAs(mvc, registerClient(mvc), provider);

		cancel(mvc, provider.token(), id, null).andExpect(status().isBadRequest());
		cancel(mvc, provider.token(), id, "  ").andExpect(status().isBadRequest());
		cancel(mvc, provider.token(), id, "Imprevisto").andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED_BY_PROVIDER"));
		ReservationHistory cancelled = historyRepository.findAll().stream()
				.filter(h -> h.getReservationId().toString().equals(id) && h.getAction() == ReservationAction.CANCELLED)
				.findFirst().orElseThrow();
		assertEquals("Imprevisto", cancelled.getReason());
		assertEquals(ReservationStatus.CANCELLED_BY_PROVIDER,
				reservationRepository.findById(java.util.UUID.fromString(id)).orElseThrow().getStatus());
	}

	@Test
	void otherProviderCannotCancel() throws Exception {
		MockMvc mvc = mvc();
		ProviderSetup provider = providerWithService(mvc, tomorrow(), NINE, ELEVEN);
		String id = bookAs(mvc, registerClient(mvc), provider);

		cancel(mvc, TestApiClient.registerProviderAndLogin(mvc), id, "Motivo").andExpect(status().isForbidden());
	}

	@Test
	void unknownReservationReturns404() throws Exception {
		MockMvc mvc = mvc();
		cancel(mvc, registerClient(mvc).token(), "00000000-0000-0000-0000-000000000000", null)
				.andExpect(status().isNotFound());
	}
}
