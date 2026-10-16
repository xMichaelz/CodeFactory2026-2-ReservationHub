package co.udea.codefactory.reservahub.reservation.controller;

import co.udea.codefactory.reservahub.reservation.DTO.AvailabilityResponseDTO;
import co.udea.codefactory.reservahub.reservation.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/services")
@Tag(name = "Availability", description = "Free time slots of a service (HU-11)")
@SecurityRequirement(name = "bearerAuth")
public class AvailabilityController {

	private final AvailabilityService availabilityService;

	public AvailabilityController(AvailabilityService availabilityService) {
		this.availabilityService = availabilityService;
	}

	@GetMapping("/{serviceId}/availability")
	@Operation(summary = "Free slots of a service between two dates, grouped by date (max 31 days)")
	public AvailabilityResponseDTO availability(@PathVariable UUID serviceId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return availabilityService.getAvailability(serviceId, from, to);
	}
}
