package co.udea.codefactory.reservahub.provider.controller;

import co.udea.codefactory.reservahub.provider.DTO.CreateProviderRequestDTO;
import co.udea.codefactory.reservahub.provider.DTO.ProviderResponseDTO;
import co.udea.codefactory.reservahub.provider.service.ProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/providers")
@Tag(name = "Providers", description = "Provider registration (HU-02)")
public class ProviderController {

	private final ProviderService providerService;

	public ProviderController(ProviderService providerService) {
		this.providerService = providerService;
	}

	@PostMapping
	@Operation(summary = "Register a provider (creates user with PROVIDER role + provider profile)")
	public ResponseEntity<ProviderResponseDTO> register(@Valid @RequestBody CreateProviderRequestDTO request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(providerService.register(request));
	}
}
