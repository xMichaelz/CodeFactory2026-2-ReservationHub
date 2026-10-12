package co.udea.codefactory.reservahub.security.service;

import co.udea.codefactory.reservahub.audit.AuditLogger;
import co.udea.codefactory.reservahub.security.DTO.LoginRequestDTO;
import co.udea.codefactory.reservahub.security.DTO.LoginResponseDTO;
import co.udea.codefactory.reservahub.security.config.LockoutProperties;
import co.udea.codefactory.reservahub.security.jwt.JwtService;
import co.udea.codefactory.reservahub.shared.enums.EntityStatus;
import co.udea.codefactory.reservahub.shared.exception.AccountLockedException;
import co.udea.codefactory.reservahub.shared.exception.BusinessException;
import co.udea.codefactory.reservahub.shared.exception.UnauthorizedException;
import co.udea.codefactory.reservahub.user.entity.User;
import co.udea.codefactory.reservahub.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final TokenRevocationService tokenRevocationService;
	private final AuditLogger auditLogger;
	private final LockoutProperties lockout;
	private final Clock clock;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
			TokenRevocationService tokenRevocationService, AuditLogger auditLogger, LockoutProperties lockout,
			Clock clock) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.tokenRevocationService = tokenRevocationService;
		this.auditLogger = auditLogger;
		this.lockout = lockout;
		this.clock = clock;
	}

	// Los fallos deben persistir aunque la excepción salga del método.
	@Transactional(noRollbackFor = BusinessException.class)
	public LoginResponseDTO login(LoginRequestDTO request) {
		User user = userRepository.findByEmailIgnoreCase(request.email().trim())
				.orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

		if (user.getStatus() != EntityStatus.ACTIVE) {
			throw new UnauthorizedException("Invalid credentials");
		}

		Instant now = clock.instant();
		if (user.getLockedUntil() != null) {
			if (now.isBefore(user.getLockedUntil())) {
				auditLogger.info("LOGIN_LOCKED", "userId=" + user.getId());
				throw new AccountLockedException(user.getLockedUntil());
			}
			user.setLockedUntil(null);
			user.setFailedAttempts(0);
		}

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			registerFailure(user, now);
			throw new UnauthorizedException("Invalid credentials");
		}

		if (user.getFailedAttempts() > 0) {
			user.setFailedAttempts(0);
			userRepository.save(user);
		}

		String token = jwtService.generateToken(user);
		auditLogger.info("LOGIN_SUCCESS", "userId=" + user.getId());
		return new LoginResponseDTO(
				token,
				"Bearer",
				jwtService.getExpirationMs(),
				user.getId(),
				user.getEmail(),
				user.getRole()
		);
	}

	private void registerFailure(User user, Instant now) {
		user.setFailedAttempts(user.getFailedAttempts() + 1);
		if (user.getFailedAttempts() >= lockout.getMaxAttempts()) {
			user.setLockedUntil(now.plus(Duration.ofMinutes(lockout.getDurationMinutes())));
			auditLogger.info("LOGIN_LOCKED", "userId=" + user.getId() + " until=" + user.getLockedUntil());
		} else {
			auditLogger.info("LOGIN_FAILED", "userId=" + user.getId() + " attempts=" + user.getFailedAttempts());
		}
		userRepository.save(user);
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
