package co.udea.codefactory.reservahub.reservation.repository;

import co.udea.codefactory.reservahub.reservation.entity.ReservationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReservationHistoryRepository extends JpaRepository<ReservationHistory, UUID> {

	List<ReservationHistory> findByReservationIdOrderByCreatedAtAsc(UUID reservationId);
}
