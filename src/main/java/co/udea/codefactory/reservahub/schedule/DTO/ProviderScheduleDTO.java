package co.udea.codefactory.reservahub.schedule.DTO;

import co.udea.codefactory.reservahub.shared.enums.DayOfWeek;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** Ventana de atención propia con sus bloqueos. */
public record ProviderScheduleDTO(
		UUID id,
		UUID providerId,
		DayOfWeek dayOfWeek,
		LocalTime startTime,
		LocalTime endTime,
		Instant createdAt,
		List<ScheduleBlockResponseDTO> blocks
) {
}
