package co.udea.codefactory.reservahub.report.dto;

import co.udea.codefactory.reservahub.report.ReservationLifecycle;
import co.udea.codefactory.reservahub.reservation.entity.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Una reserva del historial propio (HU-17) o del negocio (HU-18). */
public record ReservationHistoryItem(UUID id, UUID serviceId, String serviceName, UUID providerId,
		String providerName, UUID clientId, String clientName, LocalDate date, LocalTime startTime,
		LocalTime endTime, ReservationStatus status, ReservationLifecycle lifecycle) {
}
