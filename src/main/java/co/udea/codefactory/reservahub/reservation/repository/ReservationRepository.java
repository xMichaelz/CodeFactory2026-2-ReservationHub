package co.udea.codefactory.reservahub.reservation.repository;

import co.udea.codefactory.reservahub.reservation.entity.Reservation;
import co.udea.codefactory.reservahub.reservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

	/** Reservas del proveedor en el día con el estado dado (para el cálculo de disponibilidad). */
	List<Reservation> findByProviderIdAndReservationDateAndStatus(UUID providerId, LocalDate reservationDate,
			ReservationStatus status);

	/** Reservas confirmadas del proveedor que se cruzan con [start, end) en la fecha. */
	@Query("""
			SELECT r FROM Reservation r
			WHERE r.providerId = :providerId
			  AND r.reservationDate = :date
			  AND r.status = co.udea.codefactory.reservahub.reservation.entity.ReservationStatus.CONFIRMED
			  AND r.startTime < :end
			  AND r.endTime > :start
			""")
	List<Reservation> findConfirmedOverlappingForProvider(@Param("providerId") UUID providerId,
			@Param("date") LocalDate date, @Param("start") LocalTime start, @Param("end") LocalTime end);

	/** Reservas confirmadas del cliente que se cruzan con [start, end) en la fecha. */
	@Query("""
			SELECT r FROM Reservation r
			WHERE r.clientId = :clientId
			  AND r.reservationDate = :date
			  AND r.status = co.udea.codefactory.reservahub.reservation.entity.ReservationStatus.CONFIRMED
			  AND r.startTime < :end
			  AND r.endTime > :start
			""")
	List<Reservation> findConfirmedOverlappingForClient(@Param("clientId") UUID clientId,
			@Param("date") LocalDate date, @Param("start") LocalTime start, @Param("end") LocalTime end);
}
