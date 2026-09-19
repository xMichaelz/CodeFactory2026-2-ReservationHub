package co.udea.codefactory.reservahub.servicecatalog.mapper;

import co.udea.codefactory.reservahub.servicecatalog.DTO.ServiceResponseDTO;
import co.udea.codefactory.reservahub.servicecatalog.entity.CatalogService;
import org.springframework.stereotype.Component;

@Component
public class CatalogServiceMapper {

	public ServiceResponseDTO toResponse(CatalogService entity) {
		return new ServiceResponseDTO(
				entity.getId(),
				entity.getProviderId(),
				entity.getName(),
				entity.getDescription(),
				entity.getDurationMinutes(),
				entity.getPrice(),
				entity.getStatus(),
				entity.getCreatedAt(),
				entity.getUpdatedAt()
		);
	}
}
