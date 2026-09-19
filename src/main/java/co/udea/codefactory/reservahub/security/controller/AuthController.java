package co.udea.codefactory.reservahub.security.controller;

import co.udea.codefactory.reservahub.security.DTO.LoginRequestDTO;
import co.udea.codefactory.reservahub.security.DTO.LoginResponseDTO;
import co.udea.codefactory.reservahub.security.DTO.MessageResponseDTO;
import co.udea.codefactory.reservahub.security.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Authentication (HU-03, HU-04)")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	@Operation(summary = "Login and obtain a JWT")
	public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
		return ResponseEntity.ok(authService.login(request));
	}

	@PostMapping("/logout")
	@Operation(summary = "Logout and revoke the current JWT")
	@SecurityRequirement(name = "bearerAuth")
	public ResponseEntity<MessageResponseDTO> logout(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		authService.logout(authorization);
		return ResponseEntity.ok(new MessageResponseDTO("Logged out successfully"));
	}
}
