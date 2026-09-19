package co.udea.codefactory.reservahub.provider.mapper;

import co.udea.codefactory.reservahub.provider.DTO.ProviderResponseDTO;
import co.udea.codefactory.reservahub.provider.entity.Provider;
import co.udea.codefactory.reservahub.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ProviderMapper {

	public ProviderResponseDTO toResponse(Provider provider, User user) {
		return new ProviderResponseDTO(
				provider.getId(),
				provider.getUserId(),
				user.getEmail(),
				user.getFirstName(),
				user.getLastName(),
				provider.getBusinessName(),
				provider.getDescription(),
				provider.getAddress(),
				provider.getStatus(),
				provider.getCreatedAt()
		);
	}
}
