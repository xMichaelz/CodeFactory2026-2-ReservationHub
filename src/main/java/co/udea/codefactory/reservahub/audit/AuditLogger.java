package co.udea.codefactory.reservahub.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Minimal audit facade for Sprint 1. Persisted audit trails can be added later
 * without changing callers.
 */
@Component
public class AuditLogger {

	private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);

	public void info(String action, String details) {
		log.info("audit action={} details={}", action, details);
	}
}
