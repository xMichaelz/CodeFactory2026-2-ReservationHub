package co.udea.codefactory.reservahub.schedule.repository;

import co.udea.codefactory.reservahub.schedule.entity.ScheduleBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ScheduleBlockRepository extends JpaRepository<ScheduleBlock, UUID> {

	List<ScheduleBlock> findByScheduleIdInOrderByBlockDateAscStartTimeAsc(Collection<UUID> scheduleIds);

	List<ScheduleBlock> findByScheduleIdAndBlockDate(UUID scheduleId, LocalDate blockDate);
}
