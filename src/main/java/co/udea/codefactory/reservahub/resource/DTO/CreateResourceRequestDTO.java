package co.udea.codefactory.reservahub.resource.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateResourceRequestDTO(
		@NotBlank @Size(max = 150) String name,
		@NotBlank @Size(max = 50) String resourceType,
		@NotNull @Min(1) Integer quantity,
		@Size(max = 1000) String description
) {
}
