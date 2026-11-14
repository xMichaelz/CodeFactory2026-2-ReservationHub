package co.udea.codefactory.reservahub.audit;

import co.udea.codefactory.reservahub.AbstractIntegrationTest;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * V-12 y V-13 · La auditoría sale como una línea JSON con acción, actor, IP, resultado y traceId, sin secretos;
 * los fallos de login y los rechazos de límite incrementan contadores Micrometer; el registro de proveedores
 * deja {@code PROVIDER_REGISTERED} con IP.
 */
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class AuditLoggerTest extends AbstractIntegrationTest {

	private static final JsonMapper JSON = JsonMapper.shared();

	@Autowired
	private ApplicationContext context;
	@Autowired
	private MeterRegistry meters;
	@Autowired
	private AuditLogger auditLogger;

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = webAppContextSetup((org.springframework.web.context.WebApplicationContext) context).apply(springSecurity()).build();
	}

	@Test
	void auditEventIsJsonWithRequiredFieldsAndNoSecrets(CapturedOutput output) {
		auditLogger.event("CUSTOM_EVENT", "user-1", "203.0.113.5", "SUCCESS", "token=abc123 password=Secret123");

		JsonNode line = eventLine(output, "CUSTOM_EVENT");
		assertEquals("user-1", line.path("actor").asText());
		assertEquals("203.0.113.5", line.path("ip").asText());
		assertEquals("SUCCESS", line.path("outcome").asText());
		assertFalse(line.path("traceId").asText().isBlank());
		assertFalse(output.getAll().contains("abc123"), "el token no debe llegar al log");
		assertFalse(output.getAll().contains("Secret123"), "la contraseña no debe llegar al log");
	}

	@Test
	void auditEventWithTraceIdInMdcIsValidJsonWithSingleTraceId(CapturedOutput output) {
		org.slf4j.MDC.put("traceId", "trace-mdc-1234");
		try {
			auditLogger.event("MDC_EVENT", "user-2", "203.0.113.6", "SUCCESS", "k=v");
		} finally {
			org.slf4j.MDC.remove("traceId");
		}

		assertFalse(output.getAll().contains("has already been written"), "el formateador JSON no debe fallar");
		String raw = java.util.Arrays.stream(output.getAll().split("\\R")).map(String::trim)
				.filter(l -> l.startsWith("{") && l.contains("MDC_EVENT")).findFirst()
				.orElseThrow(() -> new AssertionError("no hay línea JSON para MDC_EVENT"));
		JsonNode line = JSON.readTree(raw);
		assertEquals("trace-mdc-1234", line.path("traceId").asText());
		assertEquals(1, raw.split("\"traceId\"", -1).length - 1, "traceId debe aparecer una sola vez");
	}

	@Test
	void failedLoginIncrementsCounterAndLogsJson(CapturedOutput output) throws Exception {
		double before = counter("reservahub.auth.login.failed");

		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"nadie-%s@example.com\",\"password\":\"Wrong999\"}".formatted(UUID.randomUUID())));

		assertEquals(before + 1, counter("reservahub.auth.login.failed"));
		JsonNode line = eventLine(output, "LOGIN_FAILED");
		assertEquals("FAILURE", line.path("outcome").asText());
		assertFalse(line.path("ip").asText().isBlank());
		assertFalse(output.getAll().contains("Wrong999"));
	}

	@Test
	void providerRegistrationLeavesEventWithIpAndCounter(CapturedOutput output) throws Exception {
		double before = counter("reservahub.provider.registered");
		String suffix = UUID.randomUUID().toString().substring(0, 8);

		mvc.perform(post("/api/v1/providers").contentType(MediaType.APPLICATION_JSON).content("""
				{"email":"prov-%s@example.com","password":"Secret123","firstName":"Ana","lastName":"Prov",
				 "businessName":"Negocio %s","serviceType":"Salud"}
				""".formatted(suffix, suffix)));

		JsonNode line = eventLine(output, "PROVIDER_REGISTERED");
		assertEquals("SUCCESS", line.path("outcome").asText());
		assertFalse(line.path("ip").asText().isBlank());
		assertTrue(line.path("ip").asText().matches("[0-9a-fA-F.:]+"), "ip real: " + line.path("ip"));
		assertEquals(before + 1, counter("reservahub.provider.registered"));
	}

	@Test
	void rateLimitCounterExists() {
		auditLogger.info("RATE_LIMITED", "rule=login ip=203.0.113.9");
		assertTrue(counter("reservahub.ratelimit.rejected") >= 1);
	}

	private double counter(String name) {
		var c = meters.find(name).counter();
		return c == null ? 0 : c.count();
	}

	private static JsonNode eventLine(CapturedOutput output, String action) {
		for (String raw : output.getAll().split("\\R")) {
			String text = raw.trim();
			if (!text.startsWith("{") || !text.contains(action)) {
				continue;
			}
			JsonNode node = JSON.readTree(text);
			if (action.equals(node.path("action").asText())) {
				assertNotNull(node.get("actor"));
				return node;
			}
		}
		throw new AssertionError("no hay línea JSON de auditoría para " + action);
	}
}
