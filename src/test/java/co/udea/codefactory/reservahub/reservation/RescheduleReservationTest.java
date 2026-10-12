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
import org.junit.jupiter.api.RepeatedTest;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static co.udea.codefactory.reservahub.reservation.ReservationTestSupport.JSON;
import static co.udea.codefactory.reservahub.reservation.ReservationTestSupport.block;
import static co.udea.codefactory.reservahub.reservation.ReservationTestSupport.book;
import static co.udea.codefactory.reservahub.reservation.ReservationTestSupport.providerWithService;
import static co.udea.codefactory.reservahub.reservation.ReservationTestSupport.registerClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU-16: reprogramar una reserva confirmada y futura a otro horario libre sin perderla. */
@SpringBootTest
class RescheduleReservationTest extends AbstractIntegrationTest {

	private static final LocalTime NINE = LocalTime.of(9, 0);
	private static final LocalTime TEN = LocalTime.of(10, 0);
	private static final LocalTime ELEVEN = LocalTime.of(11, 0);
	private static final LocalTime TWELVE = LocalTime.of(12, 0);

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

	private String bookAt(MockMvc mvc, Client client, ProviderSetup provider, LocalTime start) throws Exception {
		String body = book(mvc, client.token(), provider.serviceId(), tomorrow(), start)
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return JSON.readTree(body).get("id").asText();
	}

	private ResultActions reschedule(MockMvc mvc, String token, String id, LocalDate date, LocalTime start)
			throws Exception {
		return mvc.perform(patch("/api/v1/reservations/" + id + "/reschedule")
				.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
				.content("{\"date\":\"%s\",\"startTime\":\"%s\"}".formatted(date, start)));
	}

	private Reservation reload(String id) {
		return reservationRepository.findById(UUID.fromString(id)).orElseThrow();
	}

	private List<ReservationHistory> rescheduledHistory(String id) {
		return historyRepository.findAll().stream().filter(h -> h.getReservationId().toString().equals(id)
				&& h.getAction() == ReservationAction.RESCHEDULED).toList();
	}

	/** La reserva sigue en su horario original, confirmada y sin entrada de reprogramación. */
	private void assertUnchanged(String id, LocalTime start) {
		Reservation reservation = reload(id);
		assertEquals(tomorrow(), reservation.getReservationDate());
		assertEquals(start, reservation.getStartTime());
		assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
		assertTrue(rescheduledHistory(id).isEmpty());
	}

	@Test
	void clientMovesToFreeSlotAndHistoryKeepsOldAndNew() throws Exception {
		MockMvc mvc = mvc();
		ProviderSetup provider = providerWithService(mvc, tomorrow(), NINE, TWELVE);
		Client client = registerClient(mvc);
		String id = bookAt(mvc, client, provider, NINE);

		reschedule(mvc, client.token(), id, tomorrow(), ELEVEN).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.startTime").value("11:00:00"))
				.andExpect(jsonPath("$.endTime").value("12:00:00"))
				.andExpect(jsonPath("$.status").value("CONFIRMED"));

		List<ReservationHistory> history = rescheduledHistory(id);
		assertEquals(1, history.size());
		ReservationHistory moved = history.get(0);
		assertEquals(client.id(), moved.getActorUserId());
		assertEquals(tomorrow(), moved.getOldDate());
		assertEquals(NINE, moved.getOldStart());
		assertEquals(TEN, moved.getOldEnd());
		assertEquals(tomorrow(), moved.getNewDate());
		assertEquals(ELEVEN, moved.getNewStart());
		assertEquals(TWELVE, moved.getNewEnd());
		// El horario original queda liberado.
		mvc.perform(get("/api/v1/services/" + provider.serviceId() + "/availability?from=" + tomorrow() + "&to="
				+ tomorrow()).header("Authorization", "Bearer " + client.token())).andExpect(status().isOk())
				.andExpect(jsonPath("$.days[0].slots[0].start").value("09:00:00"));
	}

	@Test
	void providerMovesReservationOfOwnAgenda() throws Exception {
		MockMvc mvc = mvc();
		ProviderSetup provider = providerWithService(mvc, tomorrow(), NINE, TWELVE);
		String id = bookAt(mvc, registerClient(mvc), provider, NINE);

		reschedule(mvc, provider.token(), id, tomorrow(), TEN).andExpect(status().isOk())
				.andExpect(jsonPath("$.startTime").value("10:00:00"));
		assertEquals(1, rescheduledHistory(id).size());
	}

	@Test
	void movingOntoAnOverlapWithItsOwnCurrentTimeIsAllowed() throws Exception {
		MockMvc mvc = mvc();
		ProviderSetup provider = providerWithService(mvc, tomorrow(), NINE, TWELVE);
		Client client = registerClient(mvc);
		String id = bookAt(mvc, client, provider, NINE);
		// Reserva desalineada de la cuadrícula (9:30–10:30): el hueco de 10:00 solo choca con ella misma.
		Reservation reservation = reload(id);
		reservation.setStartTime(LocalTime.of(9, 30));
		reservation.setEndTime(LocalTime.of(10, 30));
		reservationRepository.save(reservation);

		reschedule(mvc, client.token(), id, tomorrow(), TEN).andExpect(status().isOk())
				.andExpect(jsonPath("$.startTime").value("10:00:00"));
	}

	@Test
	void occupiedSlotReturns409AndReservationStays() throws Exception {
		MockMvc mvc = mvc();
		ProviderSetup provider = providerWithService(mvc, tomorrow(), NINE, TWELVE);
		Client client = registerClient(mvc);
		String id = bookAt(mvc, client, provider, NINE);
		bookAt(mvc, registerClient(mvc), provider, ELEVEN);

		reschedule(mvc, client.token(), id, tomorrow(), ELEVEN).andExpect(status().isConflict());
		assertUnchanged(id, NINE);
	}

	@Test
	void blockedSlotReturns409AndReservationStays() throws Exception {
		MockMvc mvc = mvc();
		ProviderSetup provider = providerWithService(mvc, tomorrow(), NINE, TWELVE);
		Client client = registerClient(mvc);
		String id = bookAt(mvc, client, provider, NINE);
		block(mvc, provider, tomorrow(), ELEVEN, TWELVE);

		reschedule(mvc, client.token(), id, tomorrow(), ELEVEN).andExpect(status().isConflict());
		assertUnchanged(id, NINE);
	}

	@Test
	void slotOutsideTheGridReturns409() throws Exception {
		MockMvc mvc = mvc();
		ProviderSetup provider = providerWithService(mvc, tomorrow(), NINE, TWELVE);
		Client client = registerClient(mvc);
		String id = bookAt(mvc, client, provider, NINE);

		reschedule(mvc, client.token(), id, tomorrow(), LocalTime.of(10, 15)).andExpect(status().isConflict());
		assertUnchanged(id, NINE);
	}

	@Test
	void pastTargetReturns400AndReservationStays() throws Exception {
		MockMvc mvc = mvc();
		ProviderSetup provider = providerWithService(mvc, tomorrow(), NINE, TWELVE);
		Client client = registerClient(mvc);
		String id = bookAt(mvc, client, provider, NINE);

		reschedule(mvc, client.token(), id, tomorrow().minusDays(2), NINE).andExpect(status().isBadRequest());
		assertUnchanged(id, NINE);
	}

	@Test
	void clientWithAnotherReservationAtTheTargetTimeGets409() throws Exception {
		MockMvc mvc = mvc();
		ProviderSetup provider = providerWithService(mvc, tomorrow(), NINE, TWELVE);
		ProviderSetup other = providerWithService(mvc, tomorrow(), NINE, TWELVE);
		Client client = registerClient(mvc);
		String id = bookAt(mvc, client, provider, NINE);
		bookAt(mvc, client, other, ELEVEN);

		reschedule(mvc, client.token(), id, tomorrow(), ELEVEN).andExpect(status().isConflict());
		assertUnchanged(id, NINE);
	}

	@Test
	void cancelledReservationReturns409() throws Exception {
		MockMvc mvc = mvc();
		ProviderSetup provider = providerWithService(mvc, tomorrow(), NINE, TWELVE);
		Client client = registerClient(mvc);
		String id = bookAt(mvc, client, provider, NINE);
		mvc.perform(patch("/api/v1/reservations/" + id + "/cancel").header("Authorization", "Bearer " + client.token())
				.contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());

		reschedule(mvc, client.token(), id, tomorrow(), ELEVEN).andExpect(status().isConflict());
		assertTrue(rescheduledHistory(id).isEmpty());
	}

	@Test
	void reservationThatAlreadyHappenedReturns400() throws Exception {
		MockMvc mvc = mvc();
		ProviderSetup provider = providerWithService(mvc, tomorrow(), NINE, TWELVE);
		Client client = registerClient(mvc);
		String id = bookAt(mvc, client, provider, NINE);
		Reservation reservation = reload(id);
		reservation.setReservationDate(tomorrow().minusDays(3));
		reservationRepository.save(reservation);

		reschedule(mvc, client.token(), id, tomorrow(), ELEVEN).andExpect(status().isBadRequest());
		assertTrue(rescheduledHistory(id).isEmpty());
	}

	@Test
	void anotherClientCannotReschedule() throws Exception {
		MockMvc mvc = mvc();
		ProviderSetup provider = providerWithService(mvc, tomorrow(), NINE, TWELVE);
		String id = bookAt(mvc, registerClient(mvc), provider, NINE);

		reschedule(mvc, registerClient(mvc).token(), id, tomorrow(), ELEVEN).andExpect(status().isForbidden());
		assertUnchanged(id, NINE);
	}

	@Test
	void anotherProviderCannotReschedule() throws Exception {
		MockMvc mvc = mvc();
		ProviderSetup provider = providerWithService(mvc, tomorrow(), NINE, TWELVE);
		String id = bookAt(mvc, registerClient(mvc), provider, NINE);

		reschedule(mvc, TestApiClient.registerProviderAndLogin(mvc), id, tomorrow(), ELEVEN)
				.andExpect(status().isForbidden());
		assertUnchanged(id, NINE);
	}

	@Test
	void unknownReservationReturns404() throws Exception {
		MockMvc mvc = mvc();
		reschedule(mvc, registerClient(mvc).token(), UUID.randomUUID().toString(), tomorrow(), ELEVEN)
				.andExpect(status().isNotFound());
	}

	@Test
	void missingFieldsReturn400() throws Exception {
		MockMvc mvc = mvc();
		ProviderSetup provider = providerWithService(mvc, tomorrow(), NINE, TWELVE);
		Client client = registerClient(mvc);
		String id = bookAt(mvc, client, provider, NINE);

		mvc.perform(patch("/api/v1/reservations/" + id + "/reschedule").header("Authorization",
				"Bearer " + client.token()).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
	}

	@RepeatedTest(3)
	void twoReschedulesRaceForTheSameSlotAndOnlyOneWins() throws Exception {
		MockMvc setup = mvc();
		ProviderSetup provider = providerWithService(setup, tomorrow(), NINE, TWELVE);
		List<Client> clients = List.of(registerClient(setup), registerClient(setup));
		List<String> ids = List.of(bookAt(setup, clients.get(0), provider, NINE),
				bookAt(setup, clients.get(1), provider, TEN));

		CountDownLatch start = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(clients.size());
		List<Future<Integer>> results = new ArrayList<>();
		try {
			for (int i = 0; i < clients.size(); i++) {
				MockMvc mvc = mvc();
				String token = clients.get(i).token();
				String id = ids.get(i);
				results.add(pool.submit(() -> {
					start.await();
					return reschedule(mvc, token, id, tomorrow(), ELEVEN).andReturn().getResponse().getStatus();
				}));
			}
			start.countDown();
			List<Integer> statuses = new ArrayList<>();
			for (Future<Integer> result : results) {
				statuses.add(result.get(30, TimeUnit.SECONDS));
			}
			statuses.sort(Integer::compare);
			assertEquals(List.of(200, 409), statuses);
		} finally {
			pool.shutdownNow();
		}

		long atEleven = reservationRepository.findByProviderIdAndReservationDateAndStatus(
				UUID.fromString(provider.providerId()), tomorrow(), ReservationStatus.CONFIRMED).stream()
				.filter(r -> r.getStartTime().equals(ELEVEN)).count();
		assertEquals(1, atEleven);
	}
}
