package co.udea.codefactory.reservahub.user.service;

import co.udea.codefactory.reservahub.audit.AuditLogger;
import co.udea.codefactory.reservahub.shared.enums.EntityStatus;
import co.udea.codefactory.reservahub.shared.enums.Role;
import co.udea.codefactory.reservahub.shared.exception.ConflictException;
import co.udea.codefactory.reservahub.user.DTO.CreateUserRequestDTO;
import co.udea.codefactory.reservahub.user.DTO.UserResponseDTO;
import co.udea.codefactory.reservahub.user.entity.User;
import co.udea.codefactory.reservahub.user.mapper.UserMapper;
import co.udea.codefactory.reservahub.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final UserMapper userMapper;
	private final AuditLogger auditLogger;

	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, UserMapper userMapper,
			AuditLogger auditLogger) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.userMapper = userMapper;
		this.auditLogger = auditLogger;
	}

	@Transactional
	public UserResponseDTO registerClient(CreateUserRequestDTO request) {
		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new ConflictException("Email is already registered");
		}

		User user = new User();
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setFirstName(request.firstName().trim());
		user.setLastName(request.lastName().trim());
		user.setPhone(blankToNull(request.phone()));
		user.setRole(Role.CLIENT);
		user.setStatus(EntityStatus.ACTIVE);

		User saved = userRepository.save(user);
		auditLogger.info("USER_REGISTERED", "userId=" + saved.getId() + " role=CLIENT");
		return userMapper.toResponse(saved);
	}

	@Transactional
	public User createProviderAccount(String email, String rawPassword, String firstName, String lastName,
			String phone) {
		String normalized = normalizeEmail(email);
		if (userRepository.existsByEmailIgnoreCase(normalized)) {
			throw new ConflictException("Email is already registered");
		}
		User user = new User();
		user.setEmail(normalized);
		user.setPasswordHash(passwordEncoder.encode(rawPassword));
		user.setFirstName(firstName.trim());
		user.setLastName(lastName.trim());
		user.setPhone(blankToNull(phone));
		user.setRole(Role.PROVIDER);
		user.setStatus(EntityStatus.ACTIVE);
		return userRepository.save(user);
	}

	private static String normalizeEmail(String email) {
		return email.trim().toLowerCase();
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
