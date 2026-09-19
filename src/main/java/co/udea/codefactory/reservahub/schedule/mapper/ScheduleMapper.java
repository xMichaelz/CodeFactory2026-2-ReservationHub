package co.udea.codefactory.reservahub.schedule.mapper;

import co.udea.codefactory.reservahub.schedule.DTO.ScheduleBlockResponseDTO;
import co.udea.codefactory.reservahub.schedule.DTO.ScheduleResponseDTO;
import co.udea.codefactory.reservahub.schedule.entity.AttentionSchedule;
import co.udea.codefactory.reservahub.schedule.entity.ScheduleBlock;
import org.springframework.stereotype.Component;

@Component
public class ScheduleMapper {

	public ScheduleResponseDTO toResponse(AttentionSchedule schedule) {
		return new ScheduleResponseDTO(
				schedule.getId(),
				schedule.getProviderId(),
				schedule.getDayOfWeek(),
				schedule.getStartTime(),
				schedule.getEndTime(),
				schedule.getCreatedAt()
		);
	}

	public ScheduleBlockResponseDTO toBlockResponse(ScheduleBlock block) {
		return new ScheduleBlockResponseDTO(
				block.getId(),
				block.getScheduleId(),
				block.getBlockDate(),
				block.getStartTime(),
				block.getEndTime(),
				block.getReason(),
				block.getCreatedAt()
		);
	}
}
