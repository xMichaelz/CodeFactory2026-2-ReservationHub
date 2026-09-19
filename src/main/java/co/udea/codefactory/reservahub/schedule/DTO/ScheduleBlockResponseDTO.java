package co.udea.codefactory.reservahub.schedule.DTO;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ScheduleBlockResponseDTO(
		UUID id,
		UUID scheduleId,
		LocalDate blockDate,
		LocalTime startTime,
		LocalTime endTime,
		String reason,
		Instant createdAt
) {
}
