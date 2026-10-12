package co.udea.codefactory.reservahub.reservation.DTO;

import co.udea.codefactory.reservahub.reservation.entity.ReservationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Confirmación de una reserva: servicio, proveedor, fecha y hora (HU-12). */
public record ReservationResponseDTO(
		UUID id,
		UUID serviceId,
		String serviceName,
		UUID providerId,
		String providerName,
		UUID clientId,
		LocalDate date,
		LocalTime startTime,
		LocalTime endTime,
		ReservationStatus status,
		UUID createdBy,
		Instant createdAt
) {
}
