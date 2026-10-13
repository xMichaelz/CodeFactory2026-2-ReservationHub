package co.udea.codefactory.reservahub.reservation;

import co.udea.codefactory.reservahub.AbstractIntegrationTest;
import co.udea.codefactory.reservahub.provider.entity.Provider;
import co.udea.codefactory.reservahub.provider.repository.ProviderRepository;
import co.udea.codefactory.reservahub.reservation.ReservationTestSupport.Client;
import co.udea.codefactory.reservahub.reservation.ReservationTestSupport.ProviderSetup;
import co.udea.codefactory.reservahub.shared.enums.EntityStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static co.udea.codefactory.reservahub.reservation.ReservationTestSupport.JSON;
import static co.udea.codefactory.reservahub.reservation.ReservationTestSupport.book;
import static co.udea.codefactory.reservahub.reservation.ReservationTestSupport.deactivate;
import static co.udea.codefactory.reservahub.reservation.ReservationTestSupport.providerWithService;
import static co.udea.codefactory.reservahub.reservation.ReservationTestSupport.registerClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Lecturas para el frontend: catálogo de servicios y «mis reservas» / agenda del proveedor. */
@SpringBootTest
class ReadEndpointsTest extends AbstractIntegrationTest {

	private static final LocalTime NINE = LocalTime.of(9, 0);
	private static final LocalTime TEN = LocalTime.of(10, 0);
	private static final LocalTime TWELVE = LocalTime.of(12, 0);

	@Autowired
	private WebApplicationContext context;
	@Autowired
	private ProviderRepository providerRepository;
	@Value("${app.business.zone:America/Bogota}")
	private String zone;

	private MockMvc mvc() {
		return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
	}

	private JsonNode getJson(MockMvc mvc, String url, String token) throws Exception {
		String body = mvc.perform(get(url).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return JSON.readTree(body);
	}

	private static List<String> ids(JsonNode array) {
		List<String> ids = new ArrayList<>();
		array.forEach(n -> ids.add(n.get("id").asText()));
		return ids;
	}

	@Test
	void catalogListsOnlyActiveServicesOfActiveProviders() throws Exception {
		MockMvc mvc = mvc();
		LocalDate day = ReservationTestSupport.tomorrow(zone);
		ProviderSetup active = providerWithService(mvc, day, NINE, TWELVE);
		ProviderSetup inactiveService = providerWithService(mvc, day, NINE, TWELVE);
		deactivate(mvc, inactiveService);
		ProviderSetup inactiveProvider = providerWithService(mvc, day, NINE, TWELVE);
		Provider provider = providerRepository.findById(UUID.fromString(inactiveProvider.providerId())).orElseThrow();
		provider.setStatus(EntityStatus.INACTIVE);
		providerRepository.save(provider);
		Client client = registerClient(mvc);

		JsonNode catalog = getJson(mvc, "/api/v1/catalog/services", client.token());

		List<String> ids = ids(catalog);
		assertTrue(ids.contains(active.serviceId()), "falta el servicio activo");
		assertFalse(ids.contains(inactiveService.serviceId()), "aparece un servicio inactivo");
		assertFalse(ids.contains(inactiveProvider.serviceId()), "aparece el servicio de un proveedor inactivo");
		for (JsonNode entry : catalog) {
			if (entry.get("id").asText().equals(active.serviceId())) {
				assertEquals(60, entry.get("durationMinutes").asInt());
				assertEquals(active.providerId(), entry.get("providerId").asText());
				assertFalse(entry.get("providerName").asText().isBlank());
				assertFalse(entry.get("name").asText().isBlank());
			}
		}
		// El proveedor también puede ver el catálogo.
		getJson(mvc, "/api/v1/catalog/services", active.token());
	}

	@Test
	void mineReturnsOnlyTheClientsOwnReservationsInRange() throws Exception {
		MockMvc mvc = mvc();
		LocalDate day = ReservationTestSupport.tomorrow(zone);
		ProviderSetup provider = providerWithService(mvc, day, NINE, TWELVE);
		Client me = registerClient(mvc);
		Client other = registerClient(mvc);
		String mine = JSON.readTree(book(mvc, me.token(), provider.serviceId(), day, NINE)
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
		String theirs = JSON.readTree(book(mvc, other.token(), provider.serviceId(), day, TEN)
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();

		JsonNode result = getJson(mvc, "/api/v1/reservations/mine?from=%s&to=%s".formatted(day, day.plusDays(6)),
				me.token());

		assertEquals(List.of(mine), ids(result));
		assertEquals(provider.serviceId(), result.get(0).get("serviceId").asText());
		assertEquals("CONFIRMED", result.get(0).get("status").asText());
		assertFalse(ids(result).contains(theirs));

		// Fuera del rango no aparece.
		JsonNode before = getJson(mvc, "/api/v1/reservations/mine?from=%s&to=%s"
				.formatted(day.minusDays(10), day.minusDays(1)), me.token());
		assertEquals(0, before.size());
	}

	@Test
	void mineForProviderIsTheAgendaOfItsServicesSortedByTime() throws Exception {
		MockMvc mvc = mvc();
		LocalDate day = ReservationTestSupport.tomorrow(zone);
		ProviderSetup provider = providerWithService(mvc, day, NINE, TWELVE);
		ProviderSetup otherProvider = providerWithService(mvc, day, NINE, TWELVE);
		Client a = registerClient(mvc);
		Client b = registerClient(mvc);
		String late = JSON.readTree(book(mvc, a.token(), provider.serviceId(), day, TEN)
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
		String early = JSON.readTree(book(mvc, b.token(), provider.serviceId(), day, NINE)
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
		String foreign = JSON.readTree(book(mvc, a.token(), otherProvider.serviceId(), day, LocalTime.of(11, 0))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();

		JsonNode agenda = getJson(mvc, "/api/v1/reservations/mine?from=%s&to=%s".formatted(day, day),
				provider.token());

		assertEquals(List.of(early, late), ids(agenda));
		assertFalse(ids(agenda).contains(foreign));
	}

	@Test
	void mineRejectsRangesLongerThan31DaysOrReversed() throws Exception {
		MockMvc mvc = mvc();
		Client client = registerClient(mvc);
		LocalDate from = LocalDate.of(2030, 1, 1);
		mvc.perform(get("/api/v1/reservations/mine?from=%s&to=%s".formatted(from, from.plusDays(31)))
				.header("Authorization", "Bearer " + client.token()))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").exists());
		mvc.perform(get("/api/v1/reservations/mine?from=%s&to=%s".formatted(from, from.minusDays(1)))
				.header("Authorization", "Bearer " + client.token()))
				.andExpect(status().isBadRequest());
		// 31 días inclusive sí se permite.
		mvc.perform(get("/api/v1/reservations/mine?from=%s&to=%s".formatted(from, from.plusDays(30)))
				.header("Authorization", "Bearer " + client.token()))
				.andExpect(status().isOk());
	}

	@Test
	void readsWithoutTokenGet401() throws Exception {
		MockMvc mvc = mvc();
		mvc.perform(get("/api/v1/catalog/services")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/reservations/mine?from=2030-01-01&to=2030-01-02"))
				.andExpect(status().isUnauthorized());
	}
}
