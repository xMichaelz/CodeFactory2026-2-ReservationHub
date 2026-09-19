package co.udea.codefactory.reservahub.schedule.repository;

import co.udea.codefactory.reservahub.schedule.entity.AttentionSchedule;
import co.udea.codefactory.reservahub.shared.enums.DayOfWeek;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AttentionScheduleRepository extends JpaRepository<AttentionSchedule, UUID> {

	List<AttentionSchedule> findByProviderIdAndDayOfWeek(UUID providerId, DayOfWeek dayOfWeek);
}
