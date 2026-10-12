package co.udea.codefactory.reservahub.reservation.controller;

import co.udea.codefactory.reservahub.reservation.DTO.CreateReservationRequestDTO;
import co.udea.codefactory.reservahub.reservation.DTO.ReservationResponseDTO;
import co.udea.codefactory.reservahub.reservation.service.ReservationService;
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
@RequestMapping("/api/v1/reservations")
@Tag(name = "Reservations", description = "Create reservations on free time slots (HU-12)")
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
}
