package co.udea.codefactory.reservahub.schedule.DTO;

import co.udea.codefactory.reservahub.shared.enums.DayOfWeek;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record ScheduleResponseDTO(
		UUID id,
		UUID providerId,
		DayOfWeek dayOfWeek,
		LocalTime startTime,
		LocalTime endTime,
		Instant createdAt
) {
}
