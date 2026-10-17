package co.udea.codefactory.reservahub.reservation.service;

import co.udea.codefactory.reservahub.provider.repository.ProviderRepository;
import co.udea.codefactory.reservahub.reservation.DTO.CreateReservationRequestDTO;
import co.udea.codefactory.reservahub.reservation.entity.Reservation;
import co.udea.codefactory.reservahub.security.SecurityUtils.AuthenticatedUser;
import co.udea.codefactory.reservahub.servicecatalog.entity.CatalogService;
import co.udea.codefactory.reservahub.shared.enums.EntityStatus;
import co.udea.codefactory.reservahub.shared.enums.Role;
import co.udea.codefactory.reservahub.shared.exception.BusinessException;
import co.udea.codefactory.reservahub.shared.exception.ErrorCodes;
import co.udea.codefactory.reservahub.shared.exception.ForbiddenException;
import co.udea.codefactory.reservahub.shared.exception.ResourceNotFoundException;
import co.udea.codefactory.reservahub.user.entity.User;
import co.udea.codefactory.reservahub.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Reglas de quién puede crear o modificar una reserva y a nombre de qué cliente. */
@Component
class ReservationAccessPolicy {

	private final ProviderRepository providerRepository;
	private final UserRepository userRepository;

	ReservationAccessPolicy(ProviderRepository providerRepository, UserRepository userRepository) {
		this.providerRepository = providerRepository;
		this.userRepository = userRepository;
	}

	/**
	 * El rol PROVIDER actúa sobre las reservas de su agenda; cualquier otro, sobre las suyas como cliente.
	 * Devuelve si actúa como proveedor.
	 */
	boolean requireOwner(AuthenticatedUser actor, Reservation reservation) {
		boolean byProvider = actor.hasRole(Role.PROVIDER);
		if (byProvider) {
			if (!providerRepository.existsByIdAndUserId(reservation.getProviderId(), actor.userId())) {
				throw new ForbiddenException("The reservation does not belong to the provider");
			}
		} else if (!reservation.getClientId().equals(actor.userId())) {
			throw new ForbiddenException("The reservation does not belong to the client");
		}
		return byProvider;
	}

	/**
	 * El cliente solo reserva para sí. El proveedor indica el cliente (existente, activo y con rol CLIENT) y debe ser
	 * el dueño del servicio (HU-13).
	 */
	UUID clientFor(AuthenticatedUser actor, CreateReservationRequestDTO request, CatalogService service) {
		if (!actor.hasRole(Role.PROVIDER)) {
			if (request.clientId() != null && !request.clientId().equals(actor.userId())) {
				throw new ForbiddenException("A client can only create reservations for themselves");
			}
			return actor.userId();
		}
		if (!providerRepository.existsByIdAndUserId(service.getProviderId(), actor.userId())) {
			throw new ForbiddenException("The service does not belong to the provider");
		}
		if (request.clientId() == null) {
			throw badRequest("clientId is required when a provider creates a reservation");
		}
		User client = userRepository.findById(request.clientId())
				.orElseThrow(() -> new ResourceNotFoundException("Client not found"));
		if (client.getRole() != Role.CLIENT || client.getStatus() != EntityStatus.ACTIVE) {
			throw badRequest("The user is not an active client");
		}
		return client.getId();
	}

	/** El proveedor debe dar motivo al cancelar; el del cliente es opcional. Devuelve el motivo limpio o null. */
	static String cancellationReason(boolean byProvider, String rawReason) {
		String reason = rawReason == null ? null : rawReason.strip();
		if (reason != null && reason.isEmpty()) {
			reason = null;
		}
		if (byProvider && reason == null) {
			throw badRequest("A reason is required when a provider cancels a reservation");
		}
		return reason;
	}

	static BusinessException badRequest(String message) {
		return new BusinessException(ErrorCodes.BAD_REQUEST, message, HttpStatus.BAD_REQUEST.value());
	}
}
