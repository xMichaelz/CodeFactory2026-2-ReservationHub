package co.udea.codefactory.reservahub.report;

import co.udea.codefactory.reservahub.reservation.entity.Reservation;
import co.udea.codefactory.reservahub.reservation.entity.ReservationStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReservationLifecycleTest {

	private static final LocalDate DAY = LocalDate.of(2026, 11, 21);
	private static final LocalTime START = LocalTime.of(9, 0);
	private static final LocalTime END = LocalTime.of(10, 0);

	private static Reservation confirmed() {
		Reservation reservation = new Reservation();
		reservation.setReservationDate(DAY);
		reservation.setStartTime(START);
		reservation.setEndTime(END);
		reservation.setStatus(ReservationStatus.CONFIRMED);
		return reservation;
	}

	@Test
	void confirmedReservationMovesFromPendingToCompleted() {
		Reservation reservation = confirmed();

		assertEquals(ReservationLifecycle.PENDING,
				ReservationLifecycle.of(reservation, LocalDateTime.of(DAY, LocalTime.of(8, 59))));
		assertEquals(ReservationLifecycle.CONFIRMED,
				ReservationLifecycle.of(reservation, LocalDateTime.of(DAY, START)));
		assertEquals(ReservationLifecycle.COMPLETED,
				ReservationLifecycle.of(reservation, LocalDateTime.of(DAY, END)));
	}

	@Test
	void eitherCancellationBecomesCancelled() {
		Reservation reservation = confirmed();
		reservation.setStatus(ReservationStatus.CANCELLED_BY_CLIENT);
		assertEquals(ReservationLifecycle.CANCELLED,
				ReservationLifecycle.of(reservation, LocalDateTime.of(DAY, LocalTime.of(8, 0))));

		reservation.setStatus(ReservationStatus.CANCELLED_BY_PROVIDER);
		assertEquals(ReservationLifecycle.CANCELLED,
				ReservationLifecycle.of(reservation, LocalDateTime.of(DAY, END)));
	}
}
