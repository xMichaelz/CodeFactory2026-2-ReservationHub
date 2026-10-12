package co.udea.codefactory.reservahub.servicecatalog.DTO;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record LinkResourcesRequestDTO(@NotEmpty List<UUID> resourceIds) {
}
