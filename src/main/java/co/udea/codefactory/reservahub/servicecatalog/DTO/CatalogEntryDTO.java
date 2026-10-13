package co.udea.codefactory.reservahub.servicecatalog.DTO;

import java.math.BigDecimal;
import java.util.UUID;

/** Servicio activo tal como lo ve un cliente en el catálogo, con el proveedor que lo ofrece. */
public record CatalogEntryDTO(
		UUID id,
		String name,
		String description,
		Integer durationMinutes,
		BigDecimal price,
		UUID providerId,
		String providerName
) {
}
