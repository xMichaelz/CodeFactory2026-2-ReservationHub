package co.udea.codefactory.reservahub.reservation.DTO;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/** Nuevo horario de la reserva; debe ser uno de los huecos libres del servicio (HU-16). */
public record RescheduleReservationRequestDTO(
		@NotNull LocalDate date,
		@NotNull LocalTime startTime
) {
}
