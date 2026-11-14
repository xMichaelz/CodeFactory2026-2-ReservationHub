package co.udea.codefactory.reservahub.security.ratelimit;

import co.udea.codefactory.reservahub.audit.AuditLogger;
import co.udea.codefactory.reservahub.security.SecurityUtils;
import co.udea.codefactory.reservahub.shared.dto.ApiErrorResponse;
import co.udea.codefactory.reservahub.shared.exception.ErrorCodes;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Límite de peticiones (V-03). Va en la cadena de seguridad justo después del filtro JWT y antes de la
 * autorización: los endpoints públicos se cuentan por IP del cliente y los costosos por usuario autenticado
 * (el anónimo sigue hasta el 401 sin gastar cupo). Al pasarse responde 429 con {@code Retry-After} y
 * {@code errorCode = RATE_LIMITED}, y lo deja en la auditoría (regla, ruta, IP; nunca el token).
 *
 * <p>No es un {@code @Component} para que Spring Boot no lo registre además como filtro del servlet.
 */
public class RateLimitFilter extends OncePerRequestFilter {

	private static final long MAX_KEYS_PER_RULE = 100_000;

	private final boolean enabled;
	private final List<Rule> rules;
	private final ClientIpResolver ipResolver;
	private final AuditLogger auditLogger;
	private final JsonMapper jsonMapper = JsonMapper.shared();

	public RateLimitFilter(RateLimitProperties properties, AuditLogger auditLogger) {
		this.enabled = properties.enabled();
		this.ipResolver = new ClientIpResolver(properties.trustedProxies());
		this.auditLogger = auditLogger;
		PathPatternRequestMatcher.Builder path = PathPatternRequestMatcher.withDefaults();
		this.rules = List.of(
				new Rule("login", false, properties.loginPerMinute(), Duration.ofMinutes(1),
						path.matcher(HttpMethod.POST, "/api/v1/auth/login")),
				new Rule("registration", false, properties.registrationPerHour(), Duration.ofHours(1),
						path.matcher(HttpMethod.POST, "/api/v1/users")),
				new Rule("provider-registration", false, properties.providerRegistrationPerHour(), Duration.ofHours(1),
						path.matcher(HttpMethod.POST, "/api/v1/providers")),
				new Rule("lookup", true, properties.lookupPerMinute(), Duration.ofMinutes(1),
						path.matcher(HttpMethod.GET, "/api/v1/users/lookup")),
				new Rule("authenticated", true, properties.authenticatedPerMinute(), Duration.ofMinutes(1),
						new OrRequestMatcher(
								path.matcher(HttpMethod.GET, "/api/v1/services/*/availability"),
								path.matcher(HttpMethod.GET, "/api/v1/catalog/services"),
								path.matcher(HttpMethod.GET, "/api/v1/reservations/mine"))));
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !enabled;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Rule rule = rules.stream().filter(r -> r.matcher().matches(request)).findFirst().orElse(null);
		if (rule == null) {
			chain.doFilter(request, response);
			return;
		}
		String ip = ipResolver.resolve(request);
		String key = rule.perUser() ? currentUserId() : ip;
		if (key == null) { // costoso sin usuario: la autorización responderá 401
			chain.doFilter(request, response);
			return;
		}
		ConsumptionProbe probe = rule.bucketFor(key).tryConsumeAndReturnRemaining(1);
		if (probe.isConsumed()) {
			chain.doFilter(request, response);
			return;
		}
		long retryAfterSeconds = Math.max(1, (probe.getNanosToWaitForRefill() + 999_999_999L) / 1_000_000_000L);
		auditLogger.info("RATE_LIMITED", "rule=" + rule.name() + " method=" + request.getMethod()
				+ " path=" + request.getRequestURI() + " ip=" + ip
				+ (rule.perUser() ? " userId=" + key : ""));
		writeTooManyRequests(response, retryAfterSeconds);
	}

	private static String currentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof SecurityUtils.AuthenticatedUser user) {
			return user.userId().toString();
		}
		return null;
	}

	private void writeTooManyRequests(HttpServletResponse response, long retryAfterSeconds) throws IOException {
		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		jsonMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(ErrorCodes.RATE_LIMITED,
				"Too many requests, try again later", UUID.randomUUID().toString()));
	}

	/** Una regla: qué rutas cubre, si cuenta por usuario o por IP, y sus cubos (uno por clave). */
	private record Rule(String name, boolean perUser, int capacity, Duration period, RequestMatcher matcher,
			Cache<String, Bucket> buckets) {

		Rule(String name, boolean perUser, int capacity, Duration period, RequestMatcher matcher) {
			this(name, perUser, capacity, period, matcher, Caffeine.newBuilder()
					.expireAfterAccess(period)
					.maximumSize(MAX_KEYS_PER_RULE)
					.build());
		}

		Bucket bucketFor(String key) {
			return buckets.get(key, k -> Bucket.builder()
					.addLimit(Bandwidth.builder().capacity(capacity).refillIntervally(capacity, period).build())
					.build());
		}
	}
}
