package co.udea.codefactory.reservahub.reservation.service;

import co.udea.codefactory.reservahub.reservation.entity.ReservationStatus;
import co.udea.codefactory.reservahub.reservation.service.AvailabilityCalculator.AvailabilityQuery;
import co.udea.codefactory.reservahub.reservation.service.AvailabilityCalculator.BookedSlot;
import co.udea.codefactory.reservahub.reservation.service.AvailabilityCalculator.RequiredResource;
import co.udea.codefactory.reservahub.shared.time.TimeRange;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AvailabilityCalculatorTest {

	// Lunes futuro respecto a NOW.
	private static final LocalDate DAY = LocalDate.of(2026, 10, 19);
	private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 12, 8, 0);
	private static final UUID CHAIR = UUID.randomUUID();

	private static TimeRange range(int startHour, int startMin, int endHour, int endMin) {
		return new TimeRange(LocalTime.of(startHour, startMin), LocalTime.of(endHour, endMin));
	}

	private static TimeRange hours(int start, int end) {
		return range(start, 0, end, 0);
	}

	private static BookedSlot booking(TimeRange range, ReservationStatus status, UUID... resources) {
		return new BookedSlot(range, status, Set.of(resources));
	}

	private static AvailabilityQuery.Builder query() {
		return AvailabilityQuery.builder()
				.date(DAY)
				.now(NOW)
				.serviceActive(true)
				.durationMinutes(60)
				.windows(List.of(hours(9, 12)));
	}

	@Test
	void dayWithoutScheduleHasNoSlots() {
		var slots = AvailabilityCalculator.calculate(query().windows(List.of()).build());

		assertThat(slots).isEmpty();
	}

	@Test
	void slotsHaveTheServiceDuration() {
		var slots = AvailabilityCalculator.calculate(query().durationMinutes(45).windows(List.of(hours(9, 11))).build());

		assertThat(slots).containsExactly(range(9, 0, 9, 45), range(9, 45, 10, 30));
	}

	@Test
	void slotsCoverEveryWindowOfTheDay() {
		var slots = AvailabilityCalculator.calculate(query().windows(List.of(hours(14, 16), hours(9, 10))).build());

		assertThat(slots).containsExactly(hours(9, 10), hours(14, 15), hours(15, 16));
	}

	@Test
	void blockRemovesTheSlotsItTouches() {
		var slots = AvailabilityCalculator.calculate(query().blocks(List.of(range(9, 30, 10, 15))).build());

		assertThat(slots).containsExactly(hours(11, 12));
	}

	@Test
	void confirmedReservationTakesItsSlot() {
		var slots = AvailabilityCalculator.calculate(query()
				.bookings(List.of(booking(hours(10, 11), ReservationStatus.CONFIRMED)))
				.build());

		assertThat(slots).containsExactly(hours(9, 10), hours(11, 12));
	}

	@Test
	void cancelledReservationsDoNotTakeSlots() {
		var slots = AvailabilityCalculator.calculate(query()
				.bookings(List.of(
						booking(hours(10, 11), ReservationStatus.CANCELLED_BY_CLIENT),
						booking(hours(11, 12), ReservationStatus.CANCELLED_BY_PROVIDER)))
				.build());

		assertThat(slots).containsExactly(hours(9, 10), hours(10, 11), hours(11, 12));
	}

	@Test
	void resourceWithQuantityTwoAcceptsTwoOverlappingReservations() {
		var slots = AvailabilityCalculator.calculate(query()
				.resources(List.of(new RequiredResource(CHAIR, 2)))
				.bookings(List.of(booking(range(9, 30, 10, 30), ReservationStatus.CONFIRMED, CHAIR)))
				.build());

		assertThat(slots).containsExactly(hours(9, 10), hours(10, 11), hours(11, 12));
	}

	@Test
	void resourceWithQuantityTwoRejectsAThirdOverlappingReservation() {
		var slots = AvailabilityCalculator.calculate(query()
				.resources(List.of(new RequiredResource(CHAIR, 2)))
				.bookings(List.of(
						booking(hours(10, 11), ReservationStatus.CONFIRMED, CHAIR),
						booking(range(10, 30, 11, 30), ReservationStatus.CONFIRMED, CHAIR)))
				.build());

		assertThat(slots).containsExactly(hours(9, 10), hours(11, 12));
	}

	@Test
	void reservationsOnOtherResourcesDoNotConsumeCapacity() {
		var slots = AvailabilityCalculator.calculate(query()
				.resources(List.of(new RequiredResource(CHAIR, 1)))
				.bookings(List.of(booking(hours(10, 11), ReservationStatus.CONFIRMED, UUID.randomUUID())))
				.build());

		assertThat(slots).containsExactly(hours(9, 10), hours(10, 11), hours(11, 12));
	}

	@Test
	void pastDateHasNoSlots() {
		var slots = AvailabilityCalculator.calculate(query().date(NOW.toLocalDate().minusDays(1)).build());

		assertThat(slots).isEmpty();
	}

	@Test
	void todayOnlyOffersSlotsThatHaveNotStarted() {
		var slots = AvailabilityCalculator.calculate(query()
				.date(NOW.toLocalDate())
				.now(NOW.toLocalDate().atTime(10, 15))
				.build());

		assertThat(slots).containsExactly(hours(11, 12));
	}

	@Test
	void inactiveServiceHasNoSlots() {
		var slots = AvailabilityCalculator.calculate(query().serviceActive(false).build());

		assertThat(slots).isEmpty();
	}
}
