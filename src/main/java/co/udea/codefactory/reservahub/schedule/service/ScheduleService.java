package co.udea.codefactory.reservahub.schedule.service;

import co.udea.codefactory.reservahub.audit.AuditLogger;
import co.udea.codefactory.reservahub.provider.entity.Provider;
import co.udea.codefactory.reservahub.provider.service.ProviderService;
import co.udea.codefactory.reservahub.schedule.DTO.CreateScheduleBlockRequestDTO;
import co.udea.codefactory.reservahub.schedule.DTO.CreateScheduleRequestDTO;
import co.udea.codefactory.reservahub.schedule.DTO.ScheduleBlockResponseDTO;
import co.udea.codefactory.reservahub.schedule.DTO.ScheduleResponseDTO;
import co.udea.codefactory.reservahub.schedule.entity.AttentionSchedule;
import co.udea.codefactory.reservahub.schedule.entity.ScheduleBlock;
import co.udea.codefactory.reservahub.schedule.mapper.ScheduleMapper;
import co.udea.codefactory.reservahub.schedule.repository.AttentionScheduleRepository;
import co.udea.codefactory.reservahub.schedule.repository.ScheduleBlockRepository;
import co.udea.codefactory.reservahub.security.SecurityUtils;
import co.udea.codefactory.reservahub.shared.exception.BusinessException;
import co.udea.codefactory.reservahub.shared.exception.ConflictException;
import co.udea.codefactory.reservahub.shared.exception.ErrorCodes;
import co.udea.codefactory.reservahub.shared.exception.ForbiddenException;
import co.udea.codefactory.reservahub.shared.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
public class ScheduleService {

	private final AttentionScheduleRepository scheduleRepository;
	private final ScheduleBlockRepository blockRepository;
	private final ProviderService providerService;
	private final ScheduleMapper mapper;
	private final AuditLogger auditLogger;

	public ScheduleService(AttentionScheduleRepository scheduleRepository, ScheduleBlockRepository blockRepository,
			ProviderService providerService, ScheduleMapper mapper, AuditLogger auditLogger) {
		this.scheduleRepository = scheduleRepository;
		this.blockRepository = blockRepository;
		this.providerService = providerService;
		this.mapper = mapper;
		this.auditLogger = auditLogger;
	}

	@Transactional
	public ScheduleResponseDTO create(CreateScheduleRequestDTO request) {
		validateTimeRange(request.startTime(), request.endTime());
		Provider provider = currentProvider();

		List<AttentionSchedule> sameDay = scheduleRepository.findByProviderIdAndDayOfWeek(provider.getId(),
				request.dayOfWeek());
		for (AttentionSchedule existing : sameDay) {
			if (overlaps(request.startTime(), request.endTime(), existing.getStartTime(), existing.getEndTime())) {
				throw new ConflictException("Schedule overlaps an existing attention window for the same day");
			}
		}

		AttentionSchedule schedule = new AttentionSchedule();
		schedule.setProviderId(provider.getId());
		schedule.setDayOfWeek(request.dayOfWeek());
		schedule.setStartTime(request.startTime());
		schedule.setEndTime(request.endTime());

		AttentionSchedule saved = scheduleRepository.save(schedule);
		auditLogger.info("SCHEDULE_CREATED", "scheduleId=" + saved.getId() + " providerId=" + provider.getId());
		return mapper.toResponse(saved);
	}

	@Transactional
	public ScheduleBlockResponseDTO createBlock(UUID scheduleId, CreateScheduleBlockRequestDTO request) {
		validateTimeRange(request.startTime(), request.endTime());
		AttentionSchedule schedule = requireOwnedSchedule(scheduleId);

		if (!request.blockDate().getDayOfWeek().name().equals(schedule.getDayOfWeek().name())) {
			throw new BusinessException(ErrorCodes.BAD_REQUEST,
					"Block date weekday does not match the schedule day of week", HttpStatus.BAD_REQUEST.value());
		}

		if (request.startTime().isBefore(schedule.getStartTime()) || request.endTime().isAfter(schedule.getEndTime())) {
			throw new BusinessException(ErrorCodes.BAD_REQUEST,
					"Block must be within the schedule attention window", HttpStatus.BAD_REQUEST.value());
		}

		List<ScheduleBlock> sameDay = blockRepository.findByScheduleIdAndBlockDate(scheduleId, request.blockDate());
		for (ScheduleBlock existing : sameDay) {
			if (overlaps(request.startTime(), request.endTime(), existing.getStartTime(), existing.getEndTime())) {
				throw new ConflictException("Block overlaps an existing block for the same schedule and date");
			}
		}

		ScheduleBlock block = new ScheduleBlock();
		block.setScheduleId(scheduleId);
		block.setBlockDate(request.blockDate());
		block.setStartTime(request.startTime());
		block.setEndTime(request.endTime());
		block.setReason(blankToNull(request.reason()));

		ScheduleBlock saved = blockRepository.save(block);
		auditLogger.info("SCHEDULE_BLOCK_CREATED", "blockId=" + saved.getId() + " scheduleId=" + scheduleId);
		return mapper.toBlockResponse(saved);
	}

	private AttentionSchedule requireOwnedSchedule(UUID scheduleId) {
		Provider provider = currentProvider();
		AttentionSchedule schedule = scheduleRepository.findById(scheduleId)
				.orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
		if (!schedule.getProviderId().equals(provider.getId())) {
			throw new ForbiddenException("You are not allowed to modify this schedule");
		}
		return schedule;
	}

	private Provider currentProvider() {
		SecurityUtils.AuthenticatedUser user = SecurityUtils.requireCurrentUser();
		return providerService.requireByUserId(user.userId());
	}

	private static void validateTimeRange(LocalTime start, LocalTime end) {
		if (!start.isBefore(end)) {
			throw new BusinessException(ErrorCodes.BAD_REQUEST, "startTime must be before endTime",
					HttpStatus.BAD_REQUEST.value());
		}
	}

	/**
	 * Half-open style overlap: [aStart, aEnd) overlaps [bStart, bEnd) if they share any instant.
	 */
	static boolean overlaps(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
		return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
