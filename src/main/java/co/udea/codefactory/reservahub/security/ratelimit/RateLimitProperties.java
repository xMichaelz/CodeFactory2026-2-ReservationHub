package co.udea.codefactory.reservahub.security.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Límites de peticiones (V-03). Los públicos se cuentan por IP del cliente y los autenticados por usuario.
 *
 * @param enabled                      activa el límite
 * @param loginPerMinute               por IP, {@code POST /api/v1/auth/login}
 * @param registrationPerHour          por IP, {@code POST /api/v1/users}
 * @param providerRegistrationPerHour  por IP, {@code POST /api/v1/providers} (el más estricto)
 * @param authenticatedPerMinute       por usuario: disponibilidad, catálogo y {@code reservations/mine}
 * @param lookupPerMinute              por usuario, {@code GET /api/v1/users/lookup}
 * @param trustedProxies               regex de las IP de proxies de confianza (Nginx en la red de Docker);
 *                                     solo a ellas se les cree el {@code X-Forwarded-For}
 */
@ConfigurationProperties("app.rate-limit")
public record RateLimitProperties(
		@DefaultValue("true") boolean enabled,
		@DefaultValue("10") int loginPerMinute,
		@DefaultValue("20") int registrationPerHour,
		@DefaultValue("3") int providerRegistrationPerHour,
		@DefaultValue("60") int authenticatedPerMinute,
		@DefaultValue("10") int lookupPerMinute,
		@DefaultValue(DEFAULT_TRUSTED_PROXIES) String trustedProxies
) {
	/** Loopback y las redes privadas que usa Docker (172.16.0.0/12 y 192.168.0.0/16). */
	public static final String DEFAULT_TRUSTED_PROXIES =
			"127\\.\\d+\\.\\d+\\.\\d+|0:0:0:0:0:0:0:1|::1|172\\.(1[6-9]|2[0-9]|3[0-1])\\.\\d+\\.\\d+|192\\.168\\.\\d+\\.\\d+";
}
