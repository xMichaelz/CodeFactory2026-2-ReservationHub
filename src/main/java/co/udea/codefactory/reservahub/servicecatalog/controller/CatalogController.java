package co.udea.codefactory.reservahub.servicecatalog.controller;

import co.udea.codefactory.reservahub.servicecatalog.DTO.CatalogEntryDTO;
import co.udea.codefactory.reservahub.servicecatalog.service.CatalogServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog")
@Tag(name = "Catalog", description = "Bookable services for clients")
@SecurityRequirement(name = "bearerAuth")
public class CatalogController {

	private final CatalogServiceService catalogServiceService;

	public CatalogController(CatalogServiceService catalogServiceService) {
		this.catalogServiceService = catalogServiceService;
	}

	@GetMapping("/services")
	@Operation(summary = "List active services of active providers")
	public List<CatalogEntryDTO> services() {
		return catalogServiceService.listCatalog();
	}
}
