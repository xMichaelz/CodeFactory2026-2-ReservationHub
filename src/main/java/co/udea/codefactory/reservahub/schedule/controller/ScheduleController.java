package co.udea.codefactory.reservahub.schedule.controller;

import co.udea.codefactory.reservahub.schedule.DTO.CreateScheduleBlockRequestDTO;
import co.udea.codefactory.reservahub.schedule.DTO.CreateScheduleRequestDTO;
import co.udea.codefactory.reservahub.schedule.DTO.ScheduleBlockResponseDTO;
import co.udea.codefactory.reservahub.schedule.DTO.ScheduleResponseDTO;
import co.udea.codefactory.reservahub.schedule.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schedules")
@Tag(name = "Schedules", description = "Attention schedules and blocks (HU-08, HU-09)")
@SecurityRequirement(name = "bearerAuth")
public class ScheduleController {

	private final ScheduleService scheduleService;

	public ScheduleController(ScheduleService scheduleService) {
		this.scheduleService = scheduleService;
	}

	@PostMapping
	@Operation(summary = "Define an attention schedule window")
	public ResponseEntity<ScheduleResponseDTO> create(@Valid @RequestBody CreateScheduleRequestDTO request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.create(request));
	}

	@PostMapping("/{id}/blocks")
	@Operation(summary = "Block a specific date/time within a schedule")
	public ResponseEntity<ScheduleBlockResponseDTO> createBlock(@PathVariable UUID id,
			@Valid @RequestBody CreateScheduleBlockRequestDTO request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.createBlock(id, request));
	}
}
