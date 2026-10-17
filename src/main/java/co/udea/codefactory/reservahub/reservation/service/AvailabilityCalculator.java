package co.udea.codefactory.reservahub.reservation.service;

import co.udea.codefactory.reservahub.reservation.entity.ReservationStatus;
import co.udea.codefactory.reservahub.shared.time.TimeRange;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Cálculo puro de los horarios libres de un servicio en un día (sin BD ni reloj del sistema).
 *
 * <p>Reglas:
 * <ul>
 *   <li>Los huecos miden la duración del servicio y se cortan desde el inicio de cada ventana de atención.</li>
 *   <li>Un hueco que toca un bloqueo no se ofrece.</li>
 *   <li>Solo las reservas {@link ReservationStatus#CONFIRMED} ocupan.</li>
 *   <li>Cada reserva ocupa 1 unidad de cada recurso de su servicio: el hueco se llena cuando el pico de
 *       reservas simultáneas que usan un recurso alcanza su cantidad.</li>
 *   <li>Si el servicio no usa recursos, el proveedor atiende una reserva a la vez (capacidad 1 sobre todas
 *       sus reservas).</li>
 *   <li>Fechas pasadas, huecos de hoy que ya empezaron y servicios inactivos no tienen disponibilidad.</li>
 * </ul>
 * {@code date} y {@code now} deben estar en la zona del negocio ({@code app.business.zone}).
 */
public final class AvailabilityCalculator {

	private AvailabilityCalculator() {
	}

	public static List<TimeRange> calculate(AvailabilityQuery query) {
		LocalDate today = query.now().toLocalDate();
		if (!query.serviceActive() || query.durationMinutes() <= 0 || query.date().isBefore(today)) {
			return List.of();
		}
		LocalTime earliestStart = query.date().equals(today) ? query.now().toLocalTime() : null;

		List<BookedSlot> confirmed = query.bookings().stream()
				.filter(b -> b.status() == ReservationStatus.CONFIRMED)
				.toList();

		List<TimeRange> free = new ArrayList<>();
		for (TimeRange window : sortedByStart(query.windows())) {
			for (TimeRange slot : slotsOf(window, query.durationMinutes())) {
				if (isBookable(slot, earliestStart, query, confirmed)) {
					free.add(slot);
				}
			}
		}
		return free;
	}

	private static boolean isBookable(TimeRange slot, LocalTime earliestStart, AvailabilityQuery query,
			List<BookedSlot> confirmed) {
		if (earliestStart != null && !slot.start().isAfter(earliestStart)) {
			return false;
		}
		if (query.blocks().stream().anyMatch(slot::overlaps)) {
			return false;
		}
		return hasCapacity(slot, confirmed, query.resources());
	}

	private static boolean hasCapacity(TimeRange slot, List<BookedSlot> confirmed, List<RequiredResource> resources) {
		if (resources.isEmpty()) {
			return peakUsage(slot, confirmed, b -> true) < 1;
		}
		for (RequiredResource resource : resources) {
			if (peakUsage(slot, confirmed, b -> b.resourceIds().contains(resource.resourceId())) >= resource
					.quantity()) {
				return false;
			}
		}
		return true;
	}

	/** Máximo de reservas simultáneas dentro del hueco entre las que cumplen el filtro. */
	private static int peakUsage(TimeRange slot, List<BookedSlot> bookings, Predicate<BookedSlot> uses) {
		Map<LocalTime, Integer> deltas = new TreeMap<>();
		for (BookedSlot booking : bookings) {
			if (uses.test(booking) && booking.range().overlaps(slot)) {
				LocalTime start = max(booking.range().start(), slot.start());
				LocalTime end = min(booking.range().end(), slot.end());
				deltas.merge(start, 1, Integer::sum);
				deltas.merge(end, -1, Integer::sum);
			}
		}
		int current = 0;
		int peak = 0;
		// TreeMap ordena por hora; en un mismo instante se suman salidas y entradas (tramos semiabiertos).
		for (int delta : deltas.values()) {
			current += delta;
			peak = Math.max(peak, current);
		}
		return peak;
	}

	private static List<TimeRange> slotsOf(TimeRange window, int durationMinutes) {
		List<TimeRange> slots = new ArrayList<>();
		int end = window.end().toSecondOfDay();
		int step = durationMinutes * 60;
		for (int start = window.start().toSecondOfDay(); start + step <= end; start += step) {
			slots.add(new TimeRange(LocalTime.ofSecondOfDay(start), LocalTime.ofSecondOfDay((long) start + step)));
		}
		return slots;
	}

	private static List<TimeRange> sortedByStart(List<TimeRange> ranges) {
		return ranges.stream().sorted(Comparator.comparing(TimeRange::start)).toList();
	}

	private static LocalTime max(LocalTime a, LocalTime b) {
		return a.isAfter(b) ? a : b;
	}

	private static LocalTime min(LocalTime a, LocalTime b) {
		return a.isBefore(b) ? a : b;
	}

	/** Reserva existente del proveedor en el día, con los recursos que usa su servicio. */
	public record BookedSlot(TimeRange range, ReservationStatus status, Set<UUID> resourceIds) {

		public BookedSlot {
			Objects.requireNonNull(range, "range");
			Objects.requireNonNull(status, "status");
			resourceIds = resourceIds == null ? Set.of() : Set.copyOf(resourceIds);
		}
	}

	/** Recurso que necesita el servicio consultado y cuántas unidades tiene. */
	public record RequiredResource(UUID resourceId, int quantity) {
	}

	public record AvailabilityQuery(LocalDate date, LocalDateTime now, boolean serviceActive, int durationMinutes,
			List<TimeRange> windows, List<TimeRange> blocks, List<BookedSlot> bookings,
			List<RequiredResource> resources) {

		public AvailabilityQuery {
			Objects.requireNonNull(date, "date");
			Objects.requireNonNull(now, "now");
			windows = windows == null ? List.of() : List.copyOf(windows);
			blocks = blocks == null ? List.of() : List.copyOf(blocks);
			bookings = bookings == null ? List.of() : List.copyOf(bookings);
			resources = resources == null ? List.of() : List.copyOf(resources);
		}

		public static Builder builder() {
			return new Builder();
		}

		public static final class Builder {
			private LocalDate date;
			private LocalDateTime now;
			private boolean serviceActive;
			private int durationMinutes;
			private List<TimeRange> windows;
			private List<TimeRange> blocks;
			private List<BookedSlot> bookings;
			private List<RequiredResource> resources;

			private Builder() {
			}

			public Builder date(LocalDate date) {
				this.date = date;
				return this;
			}

			public Builder now(LocalDateTime now) {
				this.now = now;
				return this;
			}

			public Builder serviceActive(boolean serviceActive) {
				this.serviceActive = serviceActive;
				return this;
			}

			public Builder durationMinutes(int durationMinutes) {
				this.durationMinutes = durationMinutes;
				return this;
			}

			public Builder windows(List<TimeRange> windows) {
				this.windows = windows;
				return this;
			}

			public Builder blocks(List<TimeRange> blocks) {
				this.blocks = blocks;
				return this;
			}

			public Builder bookings(List<BookedSlot> bookings) {
				this.bookings = bookings;
				return this;
			}

			public Builder resources(List<RequiredResource> resources) {
				this.resources = resources;
				return this;
			}

			public AvailabilityQuery build() {
				return new AvailabilityQuery(date, now, serviceActive, durationMinutes, windows, blocks, bookings,
						resources);
			}
		}
	}
}
