package co.udea.codefactory.reservahub.reservation.DTO;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** {@code clientId} solo lo envía un proveedor que reserva a nombre de un cliente; el cliente reserva para sí. */
public record CreateReservationRequestDTO(
		@NotNull UUID serviceId,
		@NotNull LocalDate date,
		@NotNull LocalTime startTime,
		UUID clientId
) {
}
