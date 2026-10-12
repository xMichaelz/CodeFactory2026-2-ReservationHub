package co.udea.codefactory.reservahub.reservation.DTO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record AvailabilityResponseDTO(UUID serviceId, List<Day> days) {

	public record Day(LocalDate date, List<Slot> slots) {
	}

	public record Slot(LocalTime start, LocalTime end) {
	}
}
