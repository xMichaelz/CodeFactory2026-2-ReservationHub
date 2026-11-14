package co.udea.codefactory.reservahub.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;

import java.util.regex.Pattern;

/**
 * IP real del cliente. El {@code X-Forwarded-For} solo se cree si la petición llega desde un proxy de
 * confianza; entonces se recorre de derecha a izquierda y se toma la primera IP que no sea de un proxy.
 * Así un {@code X-Forwarded-For} inventado desde fuera no cambia la IP con la que se cuenta el límite.
 *
 * <p>En despliegue Tomcat ya hace lo mismo ({@code server.forward-headers-strategy=native} con
 * {@code server.tomcat.remoteip.internal-proxies}); este resolvedor lo repite para no depender de ello.
 */
final class ClientIpResolver {

	private final Pattern trustedProxies;

	ClientIpResolver(String trustedProxiesRegex) {
		this.trustedProxies = Pattern.compile(trustedProxiesRegex);
	}

	String resolve(HttpServletRequest request) {
		String remote = request.getRemoteAddr();
		if (remote == null || !isTrusted(remote)) {
			return remote;
		}
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded == null || forwarded.isBlank()) {
			return remote;
		}
		String[] hops = forwarded.split(",");
		String leftmost = remote;
		for (int i = hops.length - 1; i >= 0; i--) {
			String hop = hops[i].trim();
			if (hop.isEmpty()) {
				continue;
			}
			if (!isTrusted(hop)) {
				return hop;
			}
			leftmost = hop;
		}
		return leftmost;
	}

	private boolean isTrusted(String ip) {
		return trustedProxies.matcher(ip).matches();
	}
}
