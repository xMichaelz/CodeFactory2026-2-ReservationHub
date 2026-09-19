package co.udea.codefactory.reservahub.security.service;

import co.udea.codefactory.reservahub.audit.AuditLogger;
import co.udea.codefactory.reservahub.security.DTO.LoginRequestDTO;
import co.udea.codefactory.reservahub.security.DTO.LoginResponseDTO;
import co.udea.codefactory.reservahub.security.jwt.JwtService;
import co.udea.codefactory.reservahub.shared.enums.EntityStatus;
import co.udea.codefactory.reservahub.shared.exception.UnauthorizedException;
import co.udea.codefactory.reservahub.user.entity.User;
import co.udea.codefactory.reservahub.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final TokenRevocationService tokenRevocationService;
	private final AuditLogger auditLogger;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
			TokenRevocationService tokenRevocationService, AuditLogger auditLogger) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.tokenRevocationService = tokenRevocationService;
		this.auditLogger = auditLogger;
	}

	@Transactional(readOnly = true)
	public LoginResponseDTO login(LoginRequestDTO request) {
		User user = userRepository.findByEmailIgnoreCase(request.email().trim())
				.orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

		if (user.getStatus() != EntityStatus.ACTIVE) {
			throw new UnauthorizedException("Invalid credentials");
		}

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new UnauthorizedException("Invalid credentials");
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
