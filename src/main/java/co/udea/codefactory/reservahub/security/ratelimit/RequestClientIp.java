package co.udea.codefactory.reservahub.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * IP real del cliente de la petición en curso, resuelta con las mismas reglas que el límite de peticiones
 * (solo se cree el {@code X-Forwarded-For} de un proxy de confianza). La usan el bloqueo del login y la
 * auditoría. Fuera de una petición HTTP devuelve {@code "unknown"}.
 */
@Component
public class RequestClientIp {

	static final String UNKNOWN = "unknown";

	private final ClientIpResolver resolver;

	public RequestClientIp(RateLimitProperties properties) {
		this.resolver = new ClientIpResolver(properties.trustedProxies());
	}

	public String current() {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
			HttpServletRequest request = attrs.getRequest();
			String ip = resolver.resolve(request);
			return ip == null || ip.isBlank() ? UNKNOWN : ip;
		}
		return UNKNOWN;
	}
}
