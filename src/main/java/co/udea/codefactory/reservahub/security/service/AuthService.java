package co.udea.codefactory.reservahub.security.service;

import co.udea.codefactory.reservahub.audit.AuditLogger;
import co.udea.codefactory.reservahub.security.DTO.LoginRequestDTO;
import co.udea.codefactory.reservahub.security.DTO.LoginResponseDTO;
import co.udea.codefactory.reservahub.security.config.LockoutProperties;
import co.udea.codefactory.reservahub.security.jwt.JwtService;
import co.udea.codefactory.reservahub.security.ratelimit.RequestClientIp;
import co.udea.codefactory.reservahub.shared.enums.EntityStatus;
import co.udea.codefactory.reservahub.shared.exception.UnauthorizedException;
import co.udea.codefactory.reservahub.user.entity.User;
import co.udea.codefactory.reservahub.user.repository.UserRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final TokenRevocationService tokenRevocationService;
	private final AuditLogger auditLogger;
	private final LockoutProperties lockout;
	private final Clock clock;
	private final RequestClientIp clientIp;
	private final String dummyHash;
	private final Cache<String, Attempts> failures;

	private static final long MAX_TRACKED_PAIRS = 100_000;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
			TokenRevocationService tokenRevocationService, AuditLogger auditLogger, LockoutProperties lockout,
			Clock clock, RequestClientIp clientIp) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.tokenRevocationService = tokenRevocationService;
		this.auditLogger = auditLogger;
		this.lockout = lockout;
		this.clock = clock;
		this.clientIp = clientIp;
		// Hash ficticio con el mismo coste que los reales: iguala el tiempo cuando no hay cuenta que comparar.
		this.dummyHash = passwordEncoder.encode("dummy-" + UUID.randomUUID());
		// La caducidad la decide el reloj de la app; Caffeine solo limpia memoria (holgura de un minuto).
		this.failures = Caffeine.newBuilder()
				.maximumSize(MAX_TRACKED_PAIRS)
				.expireAfterWrite(Duration.ofMinutes(lockout.getDurationMinutes() + 1))
				.build();
	}

	/**
	 * Login sin enumeración ni bloqueo abusable (V-04): email inexistente, cuenta inactiva, clave errónea y
	 * pareja email+IP bloqueada responden igual (401 "Invalid credentials") y en un tiempo parecido, porque
	 * siempre se hace una comparación BCrypt (contra un hash ficticio cuando no hay cuenta o la pareja está
	 * bloqueada). El contador de fallos es por pareja email+IP y vive en memoria; las columnas
	 * {@code failed_attempts}/{@code locked_until} ya no se usan.
	 */
	@Transactional(readOnly = true)
	public LoginResponseDTO login(LoginRequestDTO request) {
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		String ip = clientIp.current();
		String key = email + "|" + ip;
		Instant now = clock.instant();

		Attempts attempts = failures.getIfPresent(key);
		if (attempts != null && attempts.isLocked(now)) {
			passwordEncoder.matches(request.password(), dummyHash);
			auditLogger.info("LOGIN_LOCKED", "ip=" + ip + " until=" + attempts.lockedUntil());
			throw invalidCredentials();
		}

		User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
		boolean passwordOk = passwordEncoder.matches(request.password(),
				user != null ? user.getPasswordHash() : dummyHash);
		if (user == null || !passwordOk || user.getStatus() != EntityStatus.ACTIVE) {
			registerFailure(key, ip, user, attempts, now);
			throw invalidCredentials();
		}

		failures.invalidate(key);
		String token = jwtService.generateToken(user);
		auditLogger.info("LOGIN_SUCCESS", "userId=" + user.getId() + " ip=" + ip);
		return new LoginResponseDTO(
				token,
				"Bearer",
				jwtService.getExpirationMs(),
				user.getId(),
				user.getEmail(),
				user.getRole()
		);
	}

	private void registerFailure(String key, String ip, User user, Attempts previous, Instant now) {
		Duration window = Duration.ofMinutes(lockout.getDurationMinutes());
		boolean stale = previous == null || previous.isExpired(now, window);
		int count = stale ? 1 : previous.count() + 1;
		Instant lockedUntil = count >= lockout.getMaxAttempts() ? now.plus(window) : null;
		failures.put(key, new Attempts(count, now, lockedUntil));
		String who = user != null ? "userId=" + user.getId() : "userId=none";
		if (lockedUntil != null) {
			auditLogger.info("LOGIN_LOCKED", who + " ip=" + ip + " until=" + lockedUntil);
		} else {
			auditLogger.info("LOGIN_FAILED", who + " ip=" + ip + " attempts=" + count);
		}
	}

	private static UnauthorizedException invalidCredentials() {
		return new UnauthorizedException("Invalid credentials");
	}

	/** Fallos de una pareja email+IP: cuántos, cuándo fue el último y hasta cuándo está bloqueada. */
	private record Attempts(int count, Instant lastFailure, Instant lockedUntil) {

		boolean isLocked(Instant now) {
			return lockedUntil != null && now.isBefore(lockedUntil);
		}

		/** La racha de fallos se olvida si pasó la ventana desde el último fallo o terminó el bloqueo. */
		boolean isExpired(Instant now, Duration window) {
			if (lockedUntil != null) {
				return !now.isBefore(lockedUntil);
			}
			return !now.isBefore(lastFailure.plus(window));
		}
	}

	@Transactional
	public void logout(String bearerToken) {
		if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
			throw new UnauthorizedException("Missing or invalid Authorization header");
		}
		String token = bearerToken.substring(7).trim();
		try {
			Claims claims = jwtService.parseClaims(token);
			tokenRevocationService.revoke(jwtService.extractJti(claims), jwtService.extractExpiration(claims));
			auditLogger.info("LOGOUT_SUCCESS", "jti=" + jwtService.extractJti(claims));
		} catch (RuntimeException ex) {
			throw new UnauthorizedException("Invalid token");
		}
	}
}
