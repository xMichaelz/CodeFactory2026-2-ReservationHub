package co.udea.codefactory.reservahub.audit;

import co.udea.codefactory.reservahub.security.ratelimit.RequestClientIp;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Auditoría estructurada (V-12): cada evento sale por el logger {@code AUDIT} con los campos {@code action},
 * {@code actor}, {@code ip}, {@code outcome}, {@code traceId} y {@code details}. Con
 * {@code logging.structured.format.console=logstash} esos campos quedan como JSON en una sola línea. También
 * cuenta en Micrometer los eventos que vigila la alerta (V-13). Los valores sensibles se enmascaran.
 */
@Component
public class AuditLogger {

	private static final Logger log = LoggerFactory.getLogger("AUDIT");
	private static final Pattern PAIR = Pattern.compile("(\\w+)=(\\S*)");
	private static final Set<String> SECRET_KEYS = Set.of("password", "pass", "token", "secret", "authorization",
			"jwt", "accesstoken");
	private static final Set<String> FAILURE_ACTIONS = Set.of("LOGIN_FAILED", "LOGIN_LOCKED", "RATE_LIMITED",
			"REGISTRATION_REJECTED");

	private final RequestClientIp clientIp;
	private final MeterRegistry meters;

	public AuditLogger(RequestClientIp clientIp, MeterRegistry meters) {
		this.clientIp = clientIp;
		this.meters = meters;
	}

	/** Compatibilidad: {@code details} son pares {@code clave=valor}; {@code ip} y el actor se extraen de ahí. */
	public void info(String action, String details) {
		String ip = null;
		String actor = null;
		StringBuilder rest = new StringBuilder();
		Matcher m = PAIR.matcher(details == null ? "" : details);
		while (m.find()) {
			String key = m.group(1);
			String value = m.group(2);
			switch (key) {
				case "ip" -> ip = value;
				case "userId", "actorId", "actor" -> {
					if (actor == null || "none".equals(actor)) {
						actor = value;
					}
				}
				default -> rest.append(rest.isEmpty() ? "" : " ").append(key).append('=').append(value);
			}
		}
		if ("none".equals(actor)) {
			actor = null;
		}
		event(action, actor, ip, FAILURE_ACTIONS.contains(action) ? "FAILURE" : "SUCCESS", rest.toString());
	}

	public void event(String action, String actor, String ip, String outcome, String details) {
		log.atInfo()
				.setMessage("audit {}")
				.addArgument(action)
				.addKeyValue("action", action)
				.addKeyValue("actor", actor == null || actor.isBlank() ? "anonymous" : actor)
				.addKeyValue("ip", ip == null || ip.isBlank() ? clientIp.current() : ip)
				.addKeyValue("outcome", outcome)
				.addKeyValue("traceId", traceId())
				.addKeyValue("details", redact(details))
				.log();
		count(action);
	}

	private void count(String action) {
		switch (action) {
			case "LOGIN_FAILED", "LOGIN_LOCKED" -> meters.counter("reservahub.auth.login.failed").increment();
			case "RATE_LIMITED" -> meters.counter("reservahub.ratelimit.rejected").increment();
			case "PROVIDER_REGISTERED" -> meters.counter("reservahub.provider.registered").increment();
			default -> {
			}
		}
	}

	private static String redact(String details) {
		if (details == null || details.isBlank()) {
			return "";
		}
		Matcher m = PAIR.matcher(details);
		StringBuilder out = new StringBuilder();
		while (m.find()) {
			String value = SECRET_KEYS.contains(m.group(1).toLowerCase()) ? "***" : m.group(2);
			m.appendReplacement(out, Matcher.quoteReplacement(m.group(1) + "=" + value));
		}
		m.appendTail(out);
		return out.toString();
	}

	/** Mismo {@code traceId} que devuelve el cuerpo de error de la petición, para poder cruzarlos. */
	private static String traceId() {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
			HttpServletRequest request = attrs.getRequest();
			Object existing = request.getAttribute("traceId");
			if (existing != null) {
				return existing.toString();
			}
			String generated = UUID.randomUUID().toString();
			request.setAttribute("traceId", generated);
			return generated;
		}
		return UUID.randomUUID().toString();
	}
}
