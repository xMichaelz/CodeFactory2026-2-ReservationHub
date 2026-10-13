package co.udea.codefactory.reservahub.servicecatalog.DTO;

import co.udea.codefactory.reservahub.resource.DTO.ResourceResponseDTO;
import co.udea.codefactory.reservahub.shared.enums.EntityStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Servicio propio del proveedor junto con los recursos asociados. */
public record ProviderServiceDTO(
		UUID id,
		UUID providerId,
		String name,
		String description,
		Integer durationMinutes,
		BigDecimal price,
		EntityStatus status,
		Instant createdAt,
		Instant updatedAt,
		List<ResourceResponseDTO> resources
) {
}
