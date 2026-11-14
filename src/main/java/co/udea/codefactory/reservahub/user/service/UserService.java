package co.udea.codefactory.reservahub.user.service;

import co.udea.codefactory.reservahub.security.SecurityUtils;
import co.udea.codefactory.reservahub.security.ratelimit.RequestClientIp;
import co.udea.codefactory.reservahub.audit.AuditLogger;
import co.udea.codefactory.reservahub.shared.enums.EntityStatus;
import co.udea.codefactory.reservahub.shared.enums.Role;
import co.udea.codefactory.reservahub.shared.exception.ConflictException;
import co.udea.codefactory.reservahub.shared.exception.ResourceNotFoundException;
import co.udea.codefactory.reservahub.user.DTO.ClientLookupDTO;
import co.udea.codefactory.reservahub.user.DTO.CreateUserRequestDTO;
import co.udea.codefactory.reservahub.user.DTO.UserResponseDTO;
import co.udea.codefactory.reservahub.user.entity.User;
import co.udea.codefactory.reservahub.user.mapper.UserMapper;
import co.udea.codefactory.reservahub.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final UserMapper userMapper;
	private final AuditLogger auditLogger;
	private final RequestClientIp clientIp;

	public static final String REGISTRATION_REJECTED_MESSAGE = "No se pudo completar el registro con los datos enviados";

	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, UserMapper userMapper,
			AuditLogger auditLogger, RequestClientIp clientIp) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.userMapper = userMapper;
		this.auditLogger = auditLogger;
		this.clientIp = clientIp;
	}

	@Transactional
	public UserResponseDTO registerClient(CreateUserRequestDTO request) {
		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw registrationRejected("email", "CLIENT");
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
			throw registrationRejected("email", "PROVIDER");
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

	/**
	 * Registro rechazado por un dato ya usado (V-05): 409 con un mensaje que no dice qué dato chocó y evento
	 * {@code REGISTRATION_REJECTED} con la IP, sin el email en claro. Riesgo residual en ADR-016.
	 */
	public ConflictException registrationRejected(String field, String role) {
		auditLogger.info("REGISTRATION_REJECTED", "role=" + role + " field=" + field + " ip=" + clientIp.current());
		return new ConflictException(REGISTRATION_REJECTED_MESSAGE);
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

	/**
	 * Búsqueda de un cliente activo por email; cualquier otro caso es 404 para no revelar qué cuentas existen.
	 * Cada búsqueda deja {@code CLIENT_LOOKUP} (actor, encontrado sí/no, SHA-256 del email) para detectar barridos.
	 */
	@Transactional(readOnly = true)
	public ClientLookupDTO lookupClient(String email) {
		String normalized = normalizeEmail(email);
		User user = userRepository.findByEmailIgnoreCase(normalized)
				.filter(u -> u.getRole() == Role.CLIENT && u.getStatus() == EntityStatus.ACTIVE)
				.orElse(null);
		String actor = SecurityUtils.currentUser().map(u -> u.userId().toString()).orElse("anonymous");
		auditLogger.info("CLIENT_LOOKUP", "actor=" + actor + " found=" + (user != null)
				+ " emailHash=" + sha256(normalized));
		if (user == null) {
			throw new ResourceNotFoundException("Client not found");
		}
		return new ClientLookupDTO(user.getId(), (user.getFirstName() + " " + user.getLastName()).trim());
	}

	private static String sha256(String value) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 not available", ex);
		}
	}
}
