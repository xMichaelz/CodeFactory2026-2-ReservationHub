package co.udea.codefactory.reservahub.user.DTO;

import co.udea.codefactory.reservahub.shared.enums.EntityStatus;
import co.udea.codefactory.reservahub.shared.enums.Role;

import java.time.Instant;
import java.util.UUID;

public record UserResponseDTO(
		UUID id,
		String email,
		String firstName,
		String lastName,
		String phone,
		Role role,
		EntityStatus status,
		Instant createdAt
) {
}
