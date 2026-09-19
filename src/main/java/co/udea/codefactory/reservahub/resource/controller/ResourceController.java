package co.udea.codefactory.reservahub.resource.controller;

import co.udea.codefactory.reservahub.resource.DTO.CreateResourceRequestDTO;
import co.udea.codefactory.reservahub.resource.DTO.ResourceResponseDTO;
import co.udea.codefactory.reservahub.resource.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/resources")
@Tag(name = "Resources", description = "Provider resources (HU-10)")
@SecurityRequirement(name = "bearerAuth")
public class ResourceController {

	private final ResourceService resourceService;

	public ResourceController(ResourceService resourceService) {
		this.resourceService = resourceService;
	}

	@PostMapping
	@Operation(summary = "Register a resource for the authenticated provider")
	public ResponseEntity<ResourceResponseDTO> create(@Valid @RequestBody CreateResourceRequestDTO request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(resourceService.create(request));
	}
}
