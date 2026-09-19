package co.udea.codefactory.reservahub.resource.mapper;

import co.udea.codefactory.reservahub.resource.DTO.ResourceResponseDTO;
import co.udea.codefactory.reservahub.resource.entity.Resource;
import org.springframework.stereotype.Component;

@Component
public class ResourceMapper {

	public ResourceResponseDTO toResponse(Resource resource) {
		return new ResourceResponseDTO(
				resource.getId(),
				resource.getProviderId(),
				resource.getName(),
				resource.getResourceType(),
				resource.getDescription(),
				resource.getStatus(),
				resource.getCreatedAt()
		);
	}
}
