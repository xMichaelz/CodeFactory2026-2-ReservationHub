package co.udea.codefactory.reservahub.shared.time;

import java.time.LocalTime;
import java.util.Objects;

/**
 * Tramo horario semiabierto [start, end) dentro de un mismo día.
 */
public record TimeRange(LocalTime start, LocalTime end) {

	public TimeRange {
		Objects.requireNonNull(start, "start");
		Objects.requireNonNull(end, "end");
	}

	public boolean overlaps(TimeRange other) {
		return overlaps(start, end, other.start, other.end);
	}

	/**
	 * Half-open style overlap: [aStart, aEnd) overlaps [bStart, bEnd) if they share any instant.
	 */
	public static boolean overlaps(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
		return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
	}
}
