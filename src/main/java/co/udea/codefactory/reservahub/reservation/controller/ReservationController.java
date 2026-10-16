package co.udea.codefactory.reservahub.reservation.controller;

import co.udea.codefactory.reservahub.reservation.DTO.CancelReservationRequestDTO;
import co.udea.codefactory.reservahub.reservation.DTO.CreateReservationRequestDTO;
import co.udea.codefactory.reservahub.reservation.DTO.RescheduleReservationRequestDTO;
import co.udea.codefactory.reservahub.reservation.DTO.ReservationResponseDTO;
import co.udea.codefactory.reservahub.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reservations")
@Tag(name = "Reservations", description = "Create, list, cancel and reschedule reservations (HU-12 to HU-16)")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

	private final ReservationService reservationService;

	public ReservationController(ReservationService reservationService) {
		this.reservationService = reservationService;
	}

	@PostMapping
	@Operation(summary = "Book a free slot of a service; 409 if the slot is taken or the client already has one")
	public ResponseEntity<ReservationResponseDTO> create(@Valid @RequestBody CreateReservationRequestDTO request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.create(request));
	}

	@GetMapping("/mine")
	@Operation(summary = "Own reservations (client) or agenda (provider) between two dates, max 31 days")
	public List<ReservationResponseDTO> mine(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return reservationService.listMine(from, to);
	}

	@PatchMapping("/{id}/cancel")
	@Operation(summary = "Cancel a future reservation; the provider must give a reason (HU-14, HU-15)")
	public ReservationResponseDTO cancel(@PathVariable UUID id,
			@Valid @RequestBody(required = false) CancelReservationRequestDTO request) {
		return reservationService.cancel(id, request);
	}

	@PatchMapping("/{id}/reschedule")
	@Operation(summary = "Move a future reservation to another free slot; 409 if the slot is not free (HU-16)")
	public ReservationResponseDTO reschedule(@PathVariable UUID id,
			@Valid @RequestBody RescheduleReservationRequestDTO request) {
		return reservationService.reschedule(id, request);
	}
}
