package co.udea.codefactory.reservahub.provider.DTO;

import co.udea.codefactory.reservahub.shared.enums.EntityStatus;

import java.time.Instant;
import java.util.UUID;

public record ProviderResponseDTO(
		UUID id,
		UUID userId,
		String email,
		String firstName,
		String lastName,
		String businessName,
		String description,
		String address,
		EntityStatus status,
		Instant createdAt
) {
}
