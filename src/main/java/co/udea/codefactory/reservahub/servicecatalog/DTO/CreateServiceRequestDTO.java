package co.udea.codefactory.reservahub.servicecatalog.DTO;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateServiceRequestDTO(
		@NotBlank @Size(max = 150) String name,
		@Size(max = 1000) String description,
		@NotNull @Positive Integer durationMinutes,
		@NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal price
) {
}
