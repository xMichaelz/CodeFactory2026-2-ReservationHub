package co.udea.codefactory.reservahub.schedule.DTO;

import co.udea.codefactory.reservahub.shared.enums.DayOfWeek;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record CreateScheduleRequestDTO(
		@NotNull DayOfWeek dayOfWeek,
		@NotNull LocalTime startTime,
		@NotNull LocalTime endTime
) {
}
