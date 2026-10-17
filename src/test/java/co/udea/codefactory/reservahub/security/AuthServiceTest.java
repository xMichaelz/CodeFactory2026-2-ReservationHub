package co.udea.codefactory.reservahub.security;

import co.udea.codefactory.reservahub.audit.AuditLogger;
import co.udea.codefactory.reservahub.security.DTO.LoginRequestDTO;
import co.udea.codefactory.reservahub.security.config.LockoutProperties;
import co.udea.codefactory.reservahub.security.jwt.JwtService;
import co.udea.codefactory.reservahub.security.service.AuthService;
import co.udea.codefactory.reservahub.security.service.TokenRevocationService;
import co.udea.codefactory.reservahub.shared.enums.EntityStatus;
import co.udea.codefactory.reservahub.shared.enums.Role;
import co.udea.codefactory.reservahub.shared.exception.BusinessException;
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
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

	private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");
	private static final LoginRequestDTO CORRECT_LOGIN = new LoginRequestDTO("ana@example.com", "Secret123");
	private static final LoginRequestDTO WRONG_PASSWORD = new LoginRequestDTO("ana@example.com", "wrong");
	private static final LoginRequestDTO UNKNOWN_EMAIL = new LoginRequestDTO("nadie@example.com", "wrong");

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = serviceAt(NOW);
	}

	private AuthService serviceAt(Instant instant) {
		LockoutProperties lockout = new LockoutProperties();
		lockout.setMaxAttempts(5);
		lockout.setDurationMinutes(15);
		return new AuthService(userRepository, passwordEncoder, jwtService, tokenRevocationService, auditLogger,
				lockout, Clock.fixed(instant, ZoneOffset.UTC));
	}

	@Test
	void loginReturnsTokenForValidCredentials() {
		User user = activeUser();
		when(userRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("Secret123", "hash")).thenReturn(true);
		when(jwtService.generateToken(user)).thenReturn("jwt-token");
		when(jwtService.getExpirationMs()).thenReturn(3600000L);

		var response = authService.login(CORRECT_LOGIN);

		assertEquals("jwt-token", response.accessToken());
		assertEquals(Role.CLIENT, response.role());
	}

	@Test
	void loginRejectsBadPassword() {
		User user = activeUser();
		when(userRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

		assertThrows(UnauthorizedException.class,
				() -> authService.login(WRONG_PASSWORD));
	}

	@Test
	void fiveFailuresLockAccountEvenForCorrectPassword() {
		User user = activeUser();
		when(userRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

		for (int i = 0; i < 5; i++) {
			assertThrows(UnauthorizedException.class,
					() -> authService.login(WRONG_PASSWORD));
		}
		assertEquals(NOW.plus(Duration.ofMinutes(15)), user.getLockedUntil());

		lenient().when(passwordEncoder.matches("Secret123", "hash")).thenReturn(true);
		BusinessException ex = assertThrows(BusinessException.class,
				() -> authService.login(CORRECT_LOGIN));
		assertEquals(ErrorCodes.ACCOUNT_LOCKED, ex.getErrorCode());
		assertEquals(423, ex.getHttpStatus());
		verify(jwtService, never()).generateToken(user);
		verify(auditLogger, times(2)).info(eq("LOGIN_LOCKED"), anyString());
	}

	@Test
	void lockedAccountDoesNotCountMoreAttempts() {
		User user = activeUser();
		user.setFailedAttempts(5);
		user.setLockedUntil(NOW.plus(Duration.ofMinutes(10)));
		when(userRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(user));

		BusinessException ex = assertThrows(BusinessException.class,
				() -> authService.login(WRONG_PASSWORD));
		assertEquals(ErrorCodes.ACCOUNT_LOCKED, ex.getErrorCode());
		assertEquals(5, user.getFailedAttempts());
		assertEquals(NOW.plus(Duration.ofMinutes(10)), user.getLockedUntil());
	}

	@Test
	void loginWorksAgainAfterLockExpires() {
		User user = activeUser();
		user.setFailedAttempts(5);
		user.setLockedUntil(NOW.plus(Duration.ofMinutes(15)));
		when(userRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("Secret123", "hash")).thenReturn(true);
		when(jwtService.generateToken(user)).thenReturn("jwt-token");

		var response = serviceAt(NOW.plus(Duration.ofMinutes(16)))
				.login(new LoginRequestDTO("ana@example.com", "Secret123"));

		assertEquals("jwt-token", response.accessToken());
		assertEquals(0, user.getFailedAttempts());
		assertNull(user.getLockedUntil());
	}

	@Test
	void failureAfterLockExpiresStartsCountingAgain() {
		User user = activeUser();
		user.setFailedAttempts(5);
		user.setLockedUntil(NOW.minus(Duration.ofMinutes(1)));
		when(userRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

		assertThrows(UnauthorizedException.class,
				() -> authService.login(WRONG_PASSWORD));
		assertEquals(1, user.getFailedAttempts());
		assertNull(user.getLockedUntil());
	}

	@Test
	void successfulLoginResetsCounter() {
		User user = activeUser();
		user.setFailedAttempts(4);
		when(userRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("Secret123", "hash")).thenReturn(true);
		when(jwtService.generateToken(user)).thenReturn("jwt-token");

		authService.login(CORRECT_LOGIN);

		assertEquals(0, user.getFailedAttempts());
		assertNull(user.getLockedUntil());
	}

	@Test
	void unknownEmailNeverRevealsLockout() {
		when(userRepository.findByEmailIgnoreCase("nadie@example.com")).thenReturn(Optional.empty());

		for (int i = 0; i < 7; i++) {
			UnauthorizedException ex = assertThrows(UnauthorizedException.class,
					() -> authService.login(UNKNOWN_EMAIL));
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
				() -> authService.login(WRONG_PASSWORD));
		assertEquals("Invalid credentials", ex.getMessage());
		assertEquals(1, user.getFailedAttempts());
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
