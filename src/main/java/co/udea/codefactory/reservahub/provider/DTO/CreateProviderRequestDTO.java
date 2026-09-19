package co.udea.codefactory.reservahub.provider.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProviderRequestDTO(
		@NotBlank @Email @Size(max = 255) String email,
		@NotBlank
		@Size(min = 8, max = 72)
		@Pattern(
				regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
				message = "must contain at least one letter and one digit"
		)
		String password,
		@NotBlank @Size(max = 100) String firstName,
		@NotBlank @Size(max = 100) String lastName,
		@Size(max = 30) String phone,
		@NotBlank @Size(max = 200) String businessName,
		@Size(max = 1000) String description,
		@Size(max = 300) String address
) {
}
