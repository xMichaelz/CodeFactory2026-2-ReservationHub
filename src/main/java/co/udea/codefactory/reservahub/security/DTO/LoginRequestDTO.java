package co.udea.codefactory.reservahub.security.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(
		@NotBlank @Email @Size(max = 255) String email,
		@NotBlank @Size(min = 1, max = 72) String password
) {
}
