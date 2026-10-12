package co.udea.codefactory.reservahub.reservation;

import co.udea.codefactory.reservahub.AbstractIntegrationTest;
import co.udea.codefactory.reservahub.reservation.ReservationTestSupport.Client;
import co.udea.codefactory.reservahub.reservation.ReservationTestSupport.ProviderSetup;
import co.udea.codefactory.reservahub.reservation.entity.ReservationStatus;
import co.udea.codefactory.reservahub.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
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

import static co.udea.codefactory.reservahub.reservation.ReservationTestSupport.book;
import static co.udea.codefactory.reservahub.reservation.ReservationTestSupport.providerWithService;
import static co.udea.codefactory.reservahub.reservation.ReservationTestSupport.registerClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/** Dos clientes piden a la vez el último cupo: solo uno lo obtiene (ADR-015). */
@SpringBootTest
class ConcurrentReservationTest extends AbstractIntegrationTest {

	@Autowired
	private WebApplicationContext context;
	@Autowired
	private ReservationRepository reservationRepository;
	@Value("${app.business.zone:America/Bogota}")
	private String zone;

	private MockMvc mvc() {
		return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
	}

	@RepeatedTest(3)
	void twoClientsRaceForTheLastSlotAndOnlyOneWins() throws Exception {
		LocalDate day = ReservationTestSupport.tomorrow(zone);
		LocalTime nine = LocalTime.of(9, 0);
		ProviderSetup provider = providerWithService(mvc(), day, nine, LocalTime.of(10, 0));
		List<Client> clients = List.of(registerClient(mvc()), registerClient(mvc()));

		CountDownLatch start = new CountDownLatch(1);
		ExecutorService pool = Executors.newFixedThreadPool(clients.size());
		List<Future<Integer>> results = new ArrayList<>();
		try {
			for (Client client : clients) {
				MockMvc mvc = mvc();
				results.add(pool.submit(() -> {
					start.await();
					return book(mvc, client.token(), provider.serviceId(), day, nine).andReturn().getResponse()
							.getStatus();
				}));
			}
			start.countDown();
			List<Integer> statuses = new ArrayList<>();
			for (Future<Integer> result : results) {
				statuses.add(result.get(30, TimeUnit.SECONDS));
			}
			statuses.sort(Integer::compare);
			assertEquals(List.of(201, 409), statuses);
		} finally {
			pool.shutdownNow();
		}

		long confirmed = reservationRepository.findByProviderIdAndReservationDateAndStatus(
				UUID.fromString(provider.providerId()), day, ReservationStatus.CONFIRMED).size();
		assertEquals(1, confirmed);
	}
}
