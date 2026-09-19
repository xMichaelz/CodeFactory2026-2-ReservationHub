package co.udea.codefactory.reservahub.resource.DTO;

import co.udea.codefactory.reservahub.shared.enums.EntityStatus;

import java.time.Instant;
import java.util.UUID;

public record ResourceResponseDTO(
		UUID id,
		UUID providerId,
		String name,
		String resourceType,
		String description,
		EntityStatus status,
		Instant createdAt
) {
}
