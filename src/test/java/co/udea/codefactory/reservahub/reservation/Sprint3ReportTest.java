package co.udea.codefactory.reservahub.reservation;

import co.udea.codefactory.reservahub.AbstractIntegrationTest;
import co.udea.codefactory.reservahub.provider.repository.ProviderRepository;
import co.udea.codefactory.reservahub.reservation.ReservationTestSupport.Client;
import co.udea.codefactory.reservahub.reservation.ReservationTestSupport.ProviderSetup;
import co.udea.codefactory.reservahub.reservation.entity.Reservation;
import co.udea.codefactory.reservahub.reservation.entity.ReservationStatus;
import co.udea.codefactory.reservahub.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

import static co.udea.codefactory.reservahub.reservation.ReservationTestSupport.JSON;
import static co.udea.codefactory.reservahub.reservation.ReservationTestSupport.book;
import static co.udea.codefactory.reservahub.reservation.ReservationTestSupport.providerWithService;
import static co.udea.codefactory.reservahub.reservation.ReservationTestSupport.registerClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Criterios de aceptación de HU-17 a HU-21. El texto vive en docs/quality/sprint-3.feature. */
@SpringBootTest
class Sprint3ReportTest extends AbstractIntegrationTest {

	private static final LocalTime NINE = LocalTime.of(9, 0);
	private static final LocalTime TEN = LocalTime.of(10, 0);
	private static final LocalTime ELEVEN = LocalTime.of(11, 0);
	private static final LocalTime TWELVE = LocalTime.of(12, 0);

	@Autowired
	private WebApplicationContext context;
	@Autowired
	private ReservationRepository reservationRepository;
	@Autowired
	private ProviderRepository providerRepository;
	@Autowired
	private JdbcTemplate jdbc;
	@Value("${app.business.zone:America/Bogota}")
	private String zone;

	private MockMvc mvc() {
		return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
	}

	private LocalDate tomorrow() {
		return ReservationTestSupport.tomorrow(zone);
	}

	private String range(LocalDate from, LocalDate to) {
		return "from=" + from + "&to=" + to;
	}

	@Test
	void clientAndProviderSeePastAndFutureReservationsWithLifecycle() throws Exception {
		MockMvc mvc = mvc();
		LocalDate tomorrow = tomorrow();
		LocalDate yesterday = LocalDate.now(ZoneId.of(zone)).minusDays(1);
		ProviderSetup provider = providerWithService(mvc, tomorrow, NINE, TWELVE);
		Client client = registerClient(mvc);
		book(mvc, client.token(), provider.serviceId(), tomorrow, NINE).andExpect(status().isCreated());
		savePastReservation(client, provider, yesterday);

		String query = range(yesterday, tomorrow);
		JsonNode mine = JSON.readTree(mvc.perform(get("/api/v1/reports/my-reservations?" + query)
				.header("Authorization", "Bearer " + client.token()))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
		assertEquals(2, mine.size());
		assertEquals("COMPLETED", mine.get(0).get("lifecycle").asText());
		assertEquals("CONFIRMED", mine.get(0).get("status").asText());
		assertEquals("PENDING", mine.get(1).get("lifecycle").asText());
		assertEquals("A C", mine.get(1).get("clientName").asText());
		assertEquals(provider.serviceId(), mine.get(1).get("serviceId").asText());
		assertFalse(mine.get(1).get("serviceName").asText().isBlank());
		assertEquals(provider.providerId(), mine.get(1).get("providerId").asText());
		assertEquals(providerRepository.findById(UUID.fromString(provider.providerId())).orElseThrow()
				.getBusinessName(), mine.get(1).get("providerName").asText());
		assertEquals(tomorrow.toString(), mine.get(1).get("date").asText());
		assertEquals("09:00:00", mine.get(1).get("startTime").asText());

		JsonNode business = JSON.readTree(mvc.perform(get("/api/v1/reports/business-reservations?" + query)
				.header("Authorization", "Bearer " + provider.token()))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
		assertEquals(2, business.size());
		assertEquals("COMPLETED", business.get(0).get("lifecycle").asText());
		assertEquals("PENDING", business.get(1).get("lifecycle").asText());
		assertEquals("A C", business.get(1).get("clientName").asText());
		assertEquals(client.id().toString(), business.get(1).get("clientId").asText());
	}

	@Test
	void demandIgnoresCancellationsAndCancellationsAreSplitByActor() throws Exception {
		MockMvc mvc = mvc();
		LocalDate tomorrow = tomorrow();
		ProviderSetup provider = providerWithService(mvc, tomorrow, NINE, TWELVE);
		Client client = registerClient(mvc);
		String byClient = idOf(book(mvc, client.token(), provider.serviceId(), tomorrow, NINE));
		String byProvider = idOf(book(mvc, client.token(), provider.serviceId(), tomorrow, TEN));
		book(mvc, client.token(), provider.serviceId(), tomorrow, ELEVEN).andExpect(status().isCreated());
		cancel(mvc, client.token(), byClient, null);
		cancel(mvc, provider.token(), byProvider, "Imprevisto");

		String query = range(tomorrow, tomorrow);
		mvc.perform(get("/api/v1/reports/demand?" + query).header("Authorization", "Bearer " + provider.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalDelivered").value(1))
				.andExpect(jsonPath("$.byService[0].serviceId").value(provider.serviceId()))
				.andExpect(jsonPath("$.byService[0].deliveredCount").value(1));

		mvc.perform(get("/api/v1/reports/cancellations?" + query + "&serviceId=" + provider.serviceId())
				.header("Authorization", "Bearer " + provider.token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cancelledByClient").value(1))
				.andExpect(jsonPath("$.cancelledByProvider").value(1))
				.andExpect(jsonPath("$.total").value(2));

		JsonNode history = JSON.readTree(mvc.perform(get("/api/v1/reports/business-reservations?" + query)
				.header("Authorization", "Bearer " + provider.token()))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
		assertEquals(3, history.size());
		assertTrue(history.valueStream().anyMatch(item -> "CANCELLED".equals(item.get("lifecycle").asText())
				&& "CANCELLED_BY_CLIENT".equals(item.get("status").asText())));
		assertTrue(history.valueStream().anyMatch(item -> "CANCELLED".equals(item.get("lifecycle").asText())
				&& "CANCELLED_BY_PROVIDER".equals(item.get("status").asText())));
	}

	@Test
	void occupancyIsOccupiedSlotsOverTheAgenda() throws Exception {
		MockMvc mvc = mvc();
		LocalDate tomorrow = tomorrow();
		ProviderSetup provider = providerWithService(mvc, tomorrow, NINE, TWELVE);
		ProviderSetup other = providerWithService(mvc, tomorrow, NINE, TWELVE);
		Client client = registerClient(mvc);
		book(mvc, client.token(), provider.serviceId(), tomorrow, NINE).andExpect(status().isCreated());

		JsonNode report = JSON.readTree(mvc.perform(get("/api/v1/reports/occupancy?" + range(tomorrow, tomorrow)
				+ "&serviceId=" + provider.serviceId()).header("Authorization", "Bearer " + provider.token()))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
		assertEquals(3, report.get("offeredSlots").asInt());
		assertEquals(1, report.get("occupiedSlots").asInt());
		assertEquals(2, report.get("freeSlots").asInt());
		assertEquals("0.3333", report.get("occupancyRate").asText());
		assertEquals(1, report.get("byDate").size());
		assertEquals(tomorrow.toString(), report.get("byDate").get(0).get("date").asText());

		mvc.perform(get("/api/v1/reports/occupancy?" + range(tomorrow, tomorrow) + "&serviceId=" + other.serviceId())
				.header("Authorization", "Bearer " + provider.token()))
				.andExpect(status().isNotFound());
	}

	@Test
	void reportsRequireTheRightRoleAndAValidRange() throws Exception {
		MockMvc mvc = mvc();
		LocalDate tomorrow = tomorrow();
		ProviderSetup provider = providerWithService(mvc, tomorrow, NINE, TWELVE);
		Client client = registerClient(mvc);
		String query = range(tomorrow, tomorrow);

		mvc.perform(get("/api/v1/reports/my-reservations?" + query)).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/reports/business-reservations?" + query)
				.header("Authorization", "Bearer " + client.token())).andExpect(status().isForbidden());
		mvc.perform(get("/api/v1/reports/my-reservations?" + query)
				.header("Authorization", "Bearer " + provider.token())).andExpect(status().isForbidden());
		mvc.perform(get("/api/v1/reports/demand?from=" + tomorrow.plusDays(1) + "&to=" + tomorrow)
				.header("Authorization", "Bearer " + provider.token())).andExpect(status().isBadRequest());
		mvc.perform(get("/api/v1/reports/demand?from=" + tomorrow + "&to=" + tomorrow.plusDays(31))
				.header("Authorization", "Bearer " + provider.token())).andExpect(status().isBadRequest());

		mvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
		mvc.perform(get("/actuator/prometheus").header("Authorization", "Bearer " + client.token()))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("jvm_")));
	}

	@Test
	void lifecycleFunctionMatchesTheReadModel() {
		assertEquals("PENDING", lifecycle("CONFIRMED", "2026-01-02 08:00:00"));
		assertEquals("CONFIRMED", lifecycle("CONFIRMED", "2026-01-02 09:30:00"));
		assertEquals("COMPLETED", lifecycle("CONFIRMED", "2026-01-02 10:00:00"));
		assertEquals("CANCELLED", lifecycle("CANCELLED_BY_CLIENT", "2026-01-02 08:00:00"));
		assertEquals("CANCELLED", lifecycle("CANCELLED_BY_PROVIDER", "2026-01-02 12:00:00"));
	}

	@Test
	void databaseRejectsReopeningACancelledReservation() throws Exception {
		MockMvc mvc = mvc();
		LocalDate tomorrow = tomorrow();
		ProviderSetup provider = providerWithService(mvc, tomorrow, NINE, TWELVE);
		Client client = registerClient(mvc);
		String id = idOf(book(mvc, client.token(), provider.serviceId(), tomorrow, NINE));
		cancel(mvc, client.token(), id, null);

		Reservation row = reservationRepository.findById(UUID.fromString(id)).orElseThrow();
		row.setStatus(ReservationStatus.CONFIRMED);
		assertThrows(DataAccessException.class, () -> reservationRepository.saveAndFlush(row));
	}

	private void savePastReservation(Client client, ProviderSetup provider, LocalDate day) {
		Reservation past = new Reservation();
		past.setClientId(client.id());
		past.setServiceId(UUID.fromString(provider.serviceId()));
		past.setProviderId(UUID.fromString(provider.providerId()));
		past.setReservationDate(day);
		past.setStartTime(NINE);
		past.setEndTime(TEN);
		past.setStatus(ReservationStatus.CONFIRMED);
		past.setCreatedBy(client.id());
		reservationRepository.saveAndFlush(past);
	}

	private String lifecycle(String status, String at) {
		return jdbc.queryForObject("""
				SELECT reservation_lifecycle(?, DATE '2026-01-02', TIME '09:00', TIME '10:00', ?::timestamp)
				""", String.class, status, at);
	}

	private static String idOf(org.springframework.test.web.servlet.ResultActions result) throws Exception {
		result.andExpect(status().isCreated());
		return JSON.readTree(result.andReturn().getResponse().getContentAsString()).get("id").asText();
	}

	private static void cancel(MockMvc mvc, String token, String id, String reason) throws Exception {
		String body = reason == null ? "{}" : "{\"reason\":\"%s\"}".formatted(reason);
		mvc.perform(patch("/api/v1/reservations/" + id + "/cancel").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
	}
}
