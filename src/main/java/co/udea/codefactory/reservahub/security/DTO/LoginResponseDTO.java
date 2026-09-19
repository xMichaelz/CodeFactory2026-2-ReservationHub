package co.udea.codefactory.reservahub.security.DTO;

import co.udea.codefactory.reservahub.shared.enums.Role;

import java.util.UUID;

public record LoginResponseDTO(
		String accessToken,
		String tokenType,
		long expiresInMs,
		UUID userId,
		String email,
		Role role
) {
}
