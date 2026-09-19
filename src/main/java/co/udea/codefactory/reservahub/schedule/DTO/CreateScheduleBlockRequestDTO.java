package co.udea.codefactory.reservahub.schedule.DTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record CreateScheduleBlockRequestDTO(
		@NotNull LocalDate blockDate,
		@NotNull LocalTime startTime,
		@NotNull LocalTime endTime,
		@Size(max = 500) String reason
) {
}
