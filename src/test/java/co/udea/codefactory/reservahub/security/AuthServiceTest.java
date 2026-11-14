package co.udea.codefactory.reservahub.security;

import co.udea.codefactory.reservahub.audit.AuditLogger;
import co.udea.codefactory.reservahub.security.DTO.LoginRequestDTO;
import co.udea.codefactory.reservahub.security.config.LockoutProperties;
import co.udea.codefactory.reservahub.security.jwt.JwtService;
import co.udea.codefactory.reservahub.security.ratelimit.RequestClientIp;
import co.udea.codefactory.reservahub.security.service.AuthService;
import co.udea.codefactory.reservahub.security.service.TokenRevocationService;
import co.udea.codefactory.reservahub.shared.enums.EntityStatus;
import co.udea.codefactory.reservahub.shared.enums.Role;
import co.udea.codefactory.reservahub.shared.exception.ErrorCodes;
import co.udea.codefactory.reservahub.shared.exception.UnauthorizedException;
import co.udea.codefactory.reservahub.user.entity.User;
import co.udea.codefactory.reservahub.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private JwtService jwtService;
	@Mock
	private TokenRevocationService tokenRevocationService;
	@Mock
	private AuditLogger auditLogger;

	@Mock
	private RequestClientIp clientIp;

	private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");
	private static final String IP_A = "203.0.113.10";
	private static final String IP_B = "203.0.113.20";

	private AuthService authService;
	private MutableClock clock;
	private String ip = IP_A;

	@BeforeEach
	void setUp() {
		clock = new MutableClock(NOW);
		lenient().when(clientIp.current()).thenAnswer(inv -> ip);
		LockoutProperties lockout = new LockoutProperties();
		lockout.setMaxAttempts(5);
		lockout.setDurationMinutes(15);
		authService = new AuthService(userRepository, passwordEncoder, jwtService, tokenRevocationService,
				auditLogger, lockout, clock, clientIp);
	}

	/** Reloj que avanza a mano para probar la caducidad del bloqueo. */
	private static final class MutableClock extends Clock {
		private Instant now;

		MutableClock(Instant now) {
			this.now = now;
		}

		void advance(Duration d) {
			now = now.plus(d);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return now;
		}
	}

	@Test
	void loginReturnsTokenForValidCredentials() {
		User user = activeUser();
		when(userRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("Secret123", "hash")).thenReturn(true);
		when(jwtService.generateToken(user)).thenReturn("jwt-token");
		when(jwtService.getExpirationMs()).thenReturn(3600000L);

		var response = authService.login(new LoginRequestDTO("ana@example.com", "Secret123"));

		assertEquals("jwt-token", response.accessToken());
		assertEquals(Role.CLIENT, response.role());
	}

	@Test
	void loginRejectsBadPassword() {
		User user = activeUser();
		when(userRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

		assertThrows(UnauthorizedException.class,
				() -> authService.login(new LoginRequestDTO("ana@example.com", "wrong")));
	}

	@Test
	void fiveFailuresFromSameIpLockThePairWithGeneric401() {
		User user = activeUser();
		when(userRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

		for (int i = 0; i < 5; i++) {
			assertThrows(UnauthorizedException.class,
					() -> authService.login(new LoginRequestDTO("ana@example.com", "wrong")));
		}

		lenient().when(passwordEncoder.matches("Secret123", "hash")).thenReturn(true);
		UnauthorizedException ex = assertThrows(UnauthorizedException.class,
				() -> authService.login(new LoginRequestDTO("ana@example.com", "Secret123")));
		assertEquals(ErrorCodes.UNAUTHORIZED, ex.getErrorCode());
		assertEquals("Invalid credentials", ex.getMessage());
		assertEquals(401, ex.getHttpStatus());
		verify(jwtService, never()).generateToken(user);
		verify(passwordEncoder, never()).matches("Secret123", "hash");
		verify(auditLogger, atLeastOnce()).info(eq("LOGIN_LOCKED"), contains("ip=" + IP_A));
	}

	@Test
	void lockOfOneIpDoesNotAffectAnotherIp() {
		User user = activeUser();
		when(userRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(user));
		lenient().when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);
		lenient().when(passwordEncoder.matches("Secret123", "hash")).thenReturn(true);
		lenient().when(jwtService.generateToken(user)).thenReturn("jwt-token");

		for (int i = 0; i < 5; i++) {
			assertThrows(UnauthorizedException.class,
					() -> authService.login(new LoginRequestDTO("ana@example.com", "wrong")));
		}
		ip = IP_B;

		assertEquals("jwt-token", authService.login(new LoginRequestDTO("ana@example.com", "Secret123")).accessToken());
	}

	@Test
	void pairLockExpiresAfterConfiguredMinutes() {
		User user = activeUser();
		when(userRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(user));
		lenient().when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);
		lenient().when(passwordEncoder.matches("Secret123", "hash")).thenReturn(true);
		lenient().when(jwtService.generateToken(user)).thenReturn("jwt-token");

		for (int i = 0; i < 5; i++) {
			assertThrows(UnauthorizedException.class,
					() -> authService.login(new LoginRequestDTO("ana@example.com", "wrong")));
		}
		clock.advance(Duration.ofMinutes(14));
		assertThrows(UnauthorizedException.class,
				() -> authService.login(new LoginRequestDTO("ana@example.com", "Secret123")));

		clock.advance(Duration.ofMinutes(2));
		assertEquals("jwt-token", authService.login(new LoginRequestDTO("ana@example.com", "Secret123")).accessToken());
	}

	@Test
	void persistedLockColumnsNoLongerBlockNorChange() {
		User user = activeUser();
		user.setFailedAttempts(5);
		user.setLockedUntil(NOW.plus(Duration.ofMinutes(10)));
		when(userRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("Secret123", "hash")).thenReturn(true);
		when(jwtService.generateToken(user)).thenReturn("jwt-token");

		assertEquals("jwt-token", authService.login(new LoginRequestDTO("ana@example.com", "Secret123")).accessToken());
		verify(userRepository, never()).save(any());
	}

	@Test
	void successfulLoginResetsPairCounter() {
		User user = activeUser();
		when(userRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(user));
		lenient().when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);
		lenient().when(passwordEncoder.matches("Secret123", "hash")).thenReturn(true);
		lenient().when(jwtService.generateToken(user)).thenReturn("jwt-token");

		for (int i = 0; i < 4; i++) {
			assertThrows(UnauthorizedException.class,
					() -> authService.login(new LoginRequestDTO("ana@example.com", "wrong")));
		}
		authService.login(new LoginRequestDTO("ana@example.com", "Secret123"));
		for (int i = 0; i < 4; i++) {
			assertThrows(UnauthorizedException.class,
					() -> authService.login(new LoginRequestDTO("ana@example.com", "wrong")));
		}
		assertEquals("jwt-token", authService.login(new LoginRequestDTO("ana@example.com", "Secret123")).accessToken());
	}

	@Test
	void unknownEmailStillRunsAPasswordComparison() {
		when(userRepository.findByEmailIgnoreCase("nadie@example.com")).thenReturn(Optional.empty());

		assertThrows(UnauthorizedException.class,
				() -> authService.login(new LoginRequestDTO("nadie@example.com", "wrong")));
		verify(passwordEncoder).matches(eq("wrong"), any());
	}

	@Test
	void unknownEmailNeverRevealsLockout() {
		when(userRepository.findByEmailIgnoreCase("nadie@example.com")).thenReturn(Optional.empty());

		for (int i = 0; i < 7; i++) {
			UnauthorizedException ex = assertThrows(UnauthorizedException.class,
					() -> authService.login(new LoginRequestDTO("nadie@example.com", "wrong")));
			assertEquals(ErrorCodes.UNAUTHORIZED, ex.getErrorCode());
			assertEquals("Invalid credentials", ex.getMessage());
		}
	}

	@Test
	void failedLoginKeepsGenericMessage() {
		User user = activeUser();
		when(userRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

		UnauthorizedException ex = assertThrows(UnauthorizedException.class,
				() -> authService.login(new LoginRequestDTO("ana@example.com", "wrong")));
		assertEquals("Invalid credentials", ex.getMessage());
		assertEquals(0, user.getFailedAttempts());
	}

	private User activeUser() {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail("ana@example.com");
		user.setPasswordHash("hash");
		user.setFirstName("Ana");
		user.setLastName("Pérez");
		user.setRole(Role.CLIENT);
		user.setStatus(EntityStatus.ACTIVE);
		return user;
	}
}
