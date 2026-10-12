package co.udea.codefactory.reservahub.reservation;

import co.udea.codefactory.reservahub.AbstractIntegrationTest;
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
import java.util.ArrayList;
import java.util.List;

import static co.udea.codefactory.reservahub.reservation.ReservationTestSupport.JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Recorrido de punta a punta del Sprint 2: de la oferta del proveedor a reservar, mover y cancelar. */
@SpringBootTest
class Sprint2AcceptanceTest extends AbstractIntegrationTest {

	@Autowired
	private WebApplicationContext context;
	@Value("${app.business.zone:America/Bogota}")
	private String zone;

	private List<String> freeStarts(MockMvc mvc, String token, String serviceId, LocalDate day) throws Exception {
		String body = mvc.perform(get("/api/v1/services/" + serviceId + "/availability?from=" + day + "&to=" + day)
				.header("Authorization", "Bearer " + token)).andExpect(status().isOk()).andReturn().getResponse()
				.getContentAsString();
		List<String> starts = new ArrayList<>();
		for (JsonNode day0 : JSON.readTree(body).get("days")) {
			day0.get("slots").forEach(s -> starts.add(s.get("start").asText()));
		}
		return starts;
	}

	@Test
	void offerAvailabilityBookConflictRescheduleCancelAndSlotReturns() throws Exception {
		MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
		LocalDate day = ReservationTestSupport.tomorrow(zone);
		var provider = ReservationTestSupport.providerWithService(mvc, day, LocalTime.of(9, 0), LocalTime.of(12, 0));
		String auth = provider.token();

		String resource = JSON.readTree(mvc.perform(post("/api/v1/resources").header("Authorization", "Bearer " + auth)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Sala unica\",\"resourceType\":\"SALA\",\"quantity\":1}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
		mvc.perform(post("/api/v1/services/" + provider.serviceId() + "/resources")
				.header("Authorization", "Bearer " + auth).contentType(MediaType.APPLICATION_JSON)
				.content("{\"resourceIds\":[\"" + resource + "\"]}")).andExpect(status().isCreated());

		var ana = ReservationTestSupport.registerClient(mvc);
		var beto = ReservationTestSupport.registerClient(mvc);
		assertEquals(List.of("09:00:00", "10:00:00", "11:00:00"), freeStarts(mvc, ana.token(), provider.serviceId(), day));

		String id = JSON.readTree(ReservationTestSupport.book(mvc, ana.token(), provider.serviceId(), day,
				LocalTime.of(9, 0)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
				.get("id").asText();
		ReservationTestSupport.book(mvc, beto.token(), provider.serviceId(), day, LocalTime.of(9, 0))
				.andExpect(status().isConflict());
		assertEquals(List.of("10:00:00", "11:00:00"), freeStarts(mvc, beto.token(), provider.serviceId(), day));

		mvc.perform(patch("/api/v1/reservations/" + id + "/reschedule").header("Authorization", "Bearer " + ana.token())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"date\":\"%s\",\"startTime\":\"10:00\"}".formatted(day))).andExpect(status().isOk());
		assertEquals(List.of("09:00:00", "11:00:00"), freeStarts(mvc, beto.token(), provider.serviceId(), day));

		mvc.perform(patch("/api/v1/reservations/" + id + "/cancel").header("Authorization", "Bearer " + ana.token())
				.contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED_BY_CLIENT"));
		assertEquals(List.of("09:00:00", "10:00:00", "11:00:00"), freeStarts(mvc, beto.token(), provider.serviceId(), day));
	}
}
