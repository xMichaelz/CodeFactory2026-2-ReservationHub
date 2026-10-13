package co.udea.codefactory.reservahub.reservation.mapper;

import co.udea.codefactory.reservahub.provider.entity.Provider;
import co.udea.codefactory.reservahub.reservation.DTO.ReservationResponseDTO;
import co.udea.codefactory.reservahub.reservation.entity.Reservation;
import co.udea.codefactory.reservahub.servicecatalog.entity.CatalogService;
import org.springframework.stereotype.Component;

@Component
public class ReservationMapper {

	public ReservationResponseDTO toResponse(Reservation entity, CatalogService service, Provider provider) {
		return new ReservationResponseDTO(
				entity.getId(),
				entity.getServiceId(),
				service.getName(),
				entity.getProviderId(),
				provider.getBusinessName(),
				entity.getClientId(),
				entity.getReservationDate(),
				entity.getStartTime(),
				entity.getEndTime(),
				entity.getStatus(),
				entity.getCreatedBy(),
				entity.getCreatedAt()
		);
	}
}
