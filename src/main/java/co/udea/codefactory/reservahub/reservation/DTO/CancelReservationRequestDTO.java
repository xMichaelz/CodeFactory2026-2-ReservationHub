package co.udea.codefactory.reservahub.reservation.DTO;

import jakarta.validation.constraints.Size;

/** El motivo es obligatorio solo cuando cancela el proveedor (HU-15). */
public record CancelReservationRequestDTO(
		@Size(max = 500) String reason
) {
}
