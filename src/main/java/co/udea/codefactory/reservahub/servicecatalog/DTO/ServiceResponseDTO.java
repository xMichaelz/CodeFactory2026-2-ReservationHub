package co.udea.codefactory.reservahub.servicecatalog.DTO;

import co.udea.codefactory.reservahub.shared.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ServiceResponseDTO(
		UUID id,
		UUID providerId,
		String name,
		String description,
		Integer durationMinutes,
		BigDecimal price,
		EntityStatus status,
		Instant createdAt,
		Instant updatedAt
) {
}
