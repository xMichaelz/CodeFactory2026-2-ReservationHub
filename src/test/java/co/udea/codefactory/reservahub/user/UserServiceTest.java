package co.udea.codefactory.reservahub.user;

import co.udea.codefactory.reservahub.audit.AuditLogger;
import co.udea.codefactory.reservahub.shared.enums.Role;
import co.udea.codefactory.reservahub.shared.exception.ConflictException;
import co.udea.codefactory.reservahub.user.DTO.CreateUserRequestDTO;
import co.udea.codefactory.reservahub.user.DTO.UserResponseDTO;
import co.udea.codefactory.reservahub.user.entity.User;
import co.udea.codefactory.reservahub.user.mapper.UserMapper;
import co.udea.codefactory.reservahub.user.repository.UserRepository;
import co.udea.codefactory.reservahub.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private AuditLogger auditLogger;

	private UserService userService;

	@BeforeEach
	void setUp() {
		userService = new UserService(userRepository, passwordEncoder, new UserMapper(), auditLogger);
	}

	@Test
	void registerClientHashesPasswordAndAssignsClientRole() {
		when(userRepository.existsByEmailIgnoreCase("ana@example.com")).thenReturn(false);
		when(passwordEncoder.encode("Secret123")).thenReturn("hashed");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
			User user = invocation.getArgument(0);
			user.setId(UUID.randomUUID());
			return user;
		});

		UserResponseDTO response = userService.registerClient(new CreateUserRequestDTO(
				"Ana@Example.com", "Secret123", "Ana", "Pérez", "3001234567"));

		assertEquals(Role.CLIENT, response.role());
		assertEquals("ana@example.com", response.email());

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(captor.capture());
		assertEquals("hashed", captor.getValue().getPasswordHash());
		assertEquals(Role.CLIENT, captor.getValue().getRole());
	}

	@Test
	void registerClientRejectsDuplicateEmail() {
		when(userRepository.existsByEmailIgnoreCase("ana@example.com")).thenReturn(true);

		assertThrows(ConflictException.class, () -> userService.registerClient(
				new CreateUserRequestDTO("ana@example.com", "Secret123", "Ana", "Pérez", null)));
	}
}
