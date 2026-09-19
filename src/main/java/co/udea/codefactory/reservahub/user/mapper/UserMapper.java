package co.udea.codefactory.reservahub.user.mapper;

import co.udea.codefactory.reservahub.user.DTO.UserResponseDTO;
import co.udea.codefactory.reservahub.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

	public UserResponseDTO toResponse(User user) {
		return new UserResponseDTO(
				user.getId(),
				user.getEmail(),
				user.getFirstName(),
				user.getLastName(),
				user.getPhone(),
				user.getRole(),
				user.getStatus(),
				user.getCreatedAt()
		);
	}
}
