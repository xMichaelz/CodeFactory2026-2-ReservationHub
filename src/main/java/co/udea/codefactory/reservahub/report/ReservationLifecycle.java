package co.udea.codefactory.reservahub.report;

import co.udea.codefactory.reservahub.reservation.entity.Reservation;
import co.udea.codefactory.reservahub.reservation.entity.ReservationStatus;

import java.time.LocalDateTime;

/**
 * Estado que ve quien consulta el historial. No se persiste: la reserva sigue en
 * {@link ReservationStatus}. La misma regla está en la función SQL {@code reservation_lifecycle}.
 *
 * <ul>
 *   <li>{@code PENDING} — confirmada y todavía no empieza (pendiente).</li>
 *   <li>{@code CONFIRMED} — confirmada y en curso.</li>
 *   <li>{@code COMPLETED} — confirmada y ya terminó (completada).</li>
 *   <li>{@code CANCELLED} — cancelada por el cliente o por el proveedor.</li>
 * </ul>
 */
public enum ReservationLifecycle {

	PENDING,
	CONFIRMED,
	COMPLETED,
	CANCELLED;

	public static ReservationLifecycle of(Reservation reservation, LocalDateTime now) {
		if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
			return CANCELLED;
		}
		LocalDateTime start = LocalDateTime.of(reservation.getReservationDate(), reservation.getStartTime());
		LocalDateTime end = LocalDateTime.of(reservation.getReservationDate(), reservation.getEndTime());
		if (!now.isBefore(end)) {
			return COMPLETED;
		}
		if (now.isBefore(start)) {
			return PENDING;
		}
		return CONFIRMED;
	}
}
