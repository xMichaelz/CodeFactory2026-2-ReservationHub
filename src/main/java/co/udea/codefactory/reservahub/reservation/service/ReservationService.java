package co.udea.codefactory.reservahub.reservation.service;

import co.udea.codefactory.reservahub.audit.AuditLogger;
import co.udea.codefactory.reservahub.provider.entity.Provider;
import co.udea.codefactory.reservahub.provider.repository.ProviderRepository;
import co.udea.codefactory.reservahub.reservation.DTO.CreateReservationRequestDTO;
import co.udea.codefactory.reservahub.reservation.DTO.ReservationResponseDTO;
import co.udea.codefactory.reservahub.reservation.entity.Reservation;
import co.udea.codefactory.reservahub.reservation.entity.ReservationAction;
import co.udea.codefactory.reservahub.reservation.entity.ReservationHistory;
import co.udea.codefactory.reservahub.reservation.entity.ReservationStatus;
import co.udea.codefactory.reservahub.reservation.mapper.ReservationMapper;
import co.udea.codefactory.reservahub.reservation.repository.ReservationHistoryRepository;
import co.udea.codefactory.reservahub.reservation.repository.ReservationRepository;
import co.udea.codefactory.reservahub.security.SecurityUtils;
import co.udea.codefactory.reservahub.security.SecurityUtils.AuthenticatedUser;
import co.udea.codefactory.reservahub.servicecatalog.entity.CatalogService;
import co.udea.codefactory.reservahub.servicecatalog.repository.CatalogServiceRepository;
import co.udea.codefactory.reservahub.shared.enums.EntityStatus;
import co.udea.codefactory.reservahub.shared.enums.Role;
import co.udea.codefactory.reservahub.shared.exception.BusinessException;
import co.udea.codefactory.reservahub.shared.exception.ConflictException;
import co.udea.codefactory.reservahub.shared.exception.ErrorCodes;
import co.udea.codefactory.reservahub.shared.exception.ForbiddenException;
import co.udea.codefactory.reservahub.shared.exception.ResourceNotFoundException;
import co.udea.codefactory.reservahub.shared.time.TimeRange;
import co.udea.codefactory.reservahub.user.entity.User;
import co.udea.codefactory.reservahub.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ReservationService {

	private final ReservationRepository reservationRepository;
	private final ReservationHistoryRepository historyRepository;
	private final CatalogServiceRepository catalogServiceRepository;
	private final ProviderRepository providerRepository;
	private final AvailabilityService availabilityService;
	private final ReservationMapper reservationMapper;
	private final AuditLogger auditLogger;
	private final UserRepository userRepository;

	public ReservationService(ReservationRepository reservationRepository,
			ReservationHistoryRepository historyRepository, CatalogServiceRepository catalogServiceRepository,
			ProviderRepository providerRepository, AvailabilityService availabilityService,
			ReservationMapper reservationMapper, AuditLogger auditLogger, UserRepository userRepository) {
		this.reservationRepository = reservationRepository;
		this.historyRepository = historyRepository;
		this.catalogServiceRepository = catalogServiceRepository;
		this.providerRepository = providerRepository;
		this.availabilityService = availabilityService;
		this.reservationMapper = reservationMapper;
		this.auditLogger = auditLogger;
		this.userRepository = userRepository;
	}

	/**
	 * Crea una reserva confirmada solo si el horario es uno de los huecos libres del servicio. El proveedor queda
	 * bloqueado ({@code SELECT … FOR UPDATE}) hasta el commit, así dos peticiones por el mismo cupo se atienden en
	 * fila y la segunda ve la reserva de la primera (ADR-015).
	 */
	@Transactional
	public ReservationResponseDTO create(CreateReservationRequestDTO request) {
		AuthenticatedUser actor = SecurityUtils.requireCurrentUser();
		CatalogService service = catalogServiceRepository.findById(request.serviceId())
				.orElseThrow(() -> new ResourceNotFoundException("Service not found"));
		UUID clientId = clientFor(actor, request, service);
		if (service.getStatus() != EntityStatus.ACTIVE) {
			throw badRequest("The service is not active");
		}
		LocalDateTime now = availabilityService.now();
		if (!request.date().atTime(request.startTime()).isAfter(now)) {
			throw badRequest("The reservation must start in the future");
		}

		Provider provider = providerRepository.findByIdForUpdate(service.getProviderId())
				.orElseThrow(() -> new ResourceNotFoundException("Provider not found"));

		TimeRange slot = availabilityService.freeSlots(service, request.date(), now).stream()
				.filter(free -> free.start().equals(request.startTime()))
				.findFirst()
				.orElseThrow(() -> new ConflictException("The requested time slot is not available"));
		if (!reservationRepository.findConfirmedOverlappingForClient(clientId, request.date(), slot.start(),
				slot.end()).isEmpty()) {
			throw new ConflictException("The client already has a reservation at that time");
		}

		Reservation reservation = new Reservation();
		reservation.setClientId(clientId);
		reservation.setServiceId(service.getId());
		reservation.setProviderId(provider.getId());
		reservation.setReservationDate(request.date());
		reservation.setStartTime(slot.start());
		reservation.setEndTime(slot.end());
		reservation.setStatus(ReservationStatus.CONFIRMED);
		reservation.setCreatedBy(actor.userId());
		Reservation saved = reservationRepository.save(reservation);

		ReservationHistory history = new ReservationHistory();
		history.setReservationId(saved.getId());
		history.setAction(ReservationAction.CREATED);
		history.setActorUserId(actor.userId());
		history.setNewDate(saved.getReservationDate());
		history.setNewStart(saved.getStartTime());
		history.setNewEnd(saved.getEndTime());
		historyRepository.save(history);

		auditLogger.info("RESERVATION_CREATED", "reservationId=" + saved.getId() + " serviceId=" + service.getId()
				+ " clientId=" + clientId + " actorId=" + actor.userId() + " date=" + saved.getReservationDate()
				+ " start=" + saved.getStartTime());
		return reservationMapper.toResponse(saved, service, provider);
	}

	/**
	 * El cliente solo reserva para sí. El proveedor indica el cliente (existente, activo y con rol CLIENT) y debe ser
	 * el dueño del servicio (HU-13).
	 */
	private UUID clientFor(AuthenticatedUser actor, CreateReservationRequestDTO request, CatalogService service) {
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

	private static BusinessException badRequest(String message) {
		return new BusinessException(ErrorCodes.BAD_REQUEST, message, HttpStatus.BAD_REQUEST.value());
	}
}
