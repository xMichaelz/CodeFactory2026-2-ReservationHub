package co.udea.codefactory.reservahub.security;

import co.udea.codefactory.reservahub.audit.AuditLogger;
import co.udea.codefactory.reservahub.security.DTO.LoginRequestDTO;
import co.udea.codefactory.reservahub.security.jwt.JwtService;
import co.udea.codefactory.reservahub.security.service.AuthService;
import co.udea.codefactory.reservahub.security.service.TokenRevocationService;
import co.udea.codefactory.reservahub.shared.enums.EntityStatus;
import co.udea.codefactory.reservahub.shared.enums.Role;
import co.udea.codefactory.reservahub.shared.exception.UnauthorizedException;
import co.udea.codefactory.reservahub.user.entity.User;
import co.udea.codefactory.reservahub.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(userRepository, passwordEncoder, jwtService, tokenRevocationService, auditLogger);
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
