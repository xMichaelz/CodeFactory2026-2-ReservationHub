package co.udea.codefactory.reservahub.servicecatalog.controller;

import co.udea.codefactory.reservahub.servicecatalog.DTO.CreateServiceRequestDTO;
import co.udea.codefactory.reservahub.servicecatalog.DTO.ServiceResponseDTO;
import co.udea.codefactory.reservahub.servicecatalog.DTO.UpdateServiceRequestDTO;
import co.udea.codefactory.reservahub.servicecatalog.service.CatalogServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/services")
@Tag(name = "Services", description = "Service catalog (HU-05, HU-06, HU-07)")
@SecurityRequirement(name = "bearerAuth")
public class CatalogServiceController {

	private final CatalogServiceService catalogServiceService;

	public CatalogServiceController(CatalogServiceService catalogServiceService) {
		this.catalogServiceService = catalogServiceService;
	}

	@PostMapping
	@Operation(summary = "Create a service for the authenticated provider")
	public ResponseEntity<ServiceResponseDTO> create(@Valid @RequestBody CreateServiceRequestDTO request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(catalogServiceService.create(request));
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update an owned service")
	public ResponseEntity<ServiceResponseDTO> update(@PathVariable UUID id,
			@Valid @RequestBody UpdateServiceRequestDTO request) {
		return ResponseEntity.ok(catalogServiceService.update(id, request));
	}

	@PatchMapping("/{id}/deactivate")
	@Operation(summary = "Logically deactivate an owned service")
	public ResponseEntity<ServiceResponseDTO> deactivate(@PathVariable UUID id) {
		return ResponseEntity.ok(catalogServiceService.deactivate(id));
	}
}
