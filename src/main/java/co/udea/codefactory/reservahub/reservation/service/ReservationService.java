package co.udea.codefactory.reservahub.reservation.service;

import co.udea.codefactory.reservahub.audit.AuditLogger;
import co.udea.codefactory.reservahub.provider.entity.Provider;
import co.udea.codefactory.reservahub.provider.repository.ProviderRepository;
import co.udea.codefactory.reservahub.reservation.DTO.CancelReservationRequestDTO;
import co.udea.codefactory.reservahub.reservation.DTO.CreateReservationRequestDTO;
import co.udea.codefactory.reservahub.reservation.DTO.RescheduleReservationRequestDTO;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReservationService {

	static final int MAX_RANGE_DAYS = 31;

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
	 * Cancela una reserva confirmada y futura. El cliente cancela las suyas; el proveedor las de su agenda y con
	 * motivo obligatorio (HU-14, HU-15). El horario vuelve a salir libre porque la disponibilidad solo cuenta
	 * reservas confirmadas.
	 */
	@Transactional
	public ReservationResponseDTO cancel(UUID id, CancelReservationRequestDTO request) {
		AuthenticatedUser actor = SecurityUtils.requireCurrentUser();
		Reservation reservation = reservationRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
		boolean byProvider = requireOwner(actor, reservation);
		if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
			throw new ConflictException("The reservation is already cancelled");
		}
		if (!reservation.getReservationDate().atTime(reservation.getStartTime()).isAfter(availabilityService.now())) {
			throw badRequest("Only future reservations can be cancelled");
		}
		String reason = request == null || request.reason() == null ? null : request.reason().strip();
		if (byProvider && (reason == null || reason.isEmpty())) {
			throw badRequest("A reason is required when a provider cancels a reservation");
		}

		reservation.setStatus(byProvider ? ReservationStatus.CANCELLED_BY_PROVIDER
				: ReservationStatus.CANCELLED_BY_CLIENT);
		Reservation saved = reservationRepository.save(reservation);

		ReservationHistory history = new ReservationHistory();
		history.setReservationId(saved.getId());
		history.setAction(ReservationAction.CANCELLED);
		history.setActorUserId(actor.userId());
		history.setReason(reason == null || reason.isEmpty() ? null : reason);
		history.setOldDate(saved.getReservationDate());
		history.setOldStart(saved.getStartTime());
		history.setOldEnd(saved.getEndTime());
		historyRepository.save(history);

		auditLogger.info("RESERVATION_CANCELLED", "reservationId=" + saved.getId() + " actorId=" + actor.userId()
				+ " status=" + saved.getStatus());
		CatalogService service = catalogServiceRepository.findById(saved.getServiceId())
				.orElseThrow(() -> new ResourceNotFoundException("Service not found"));
		Provider provider = providerRepository.findById(saved.getProviderId())
				.orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
		return reservationMapper.toResponse(saved, service, provider);
	}

	/**
	 * Mueve una reserva confirmada y futura a otro hueco libre del mismo servicio (HU-16). Con el proveedor
	 * bloqueado se recalcula la disponibilidad sin contar la propia reserva; si el hueco no está libre la reserva
	 * queda como estaba. {@code @Version} hace que una edición simultánea de la misma fila termine en 409.
	 */
	@Transactional
	public ReservationResponseDTO reschedule(UUID id, RescheduleReservationRequestDTO request) {
		AuthenticatedUser actor = SecurityUtils.requireCurrentUser();
		Reservation reservation = reservationRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
		requireOwner(actor, reservation);
		if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
			throw new ConflictException("Only confirmed reservations can be rescheduled");
		}
		LocalDateTime now = availabilityService.now();
		if (!reservation.getReservationDate().atTime(reservation.getStartTime()).isAfter(now)) {
			throw badRequest("Only future reservations can be rescheduled");
		}
		if (!request.date().atTime(request.startTime()).isAfter(now)) {
			throw badRequest("The new time must be in the future");
		}
		CatalogService service = catalogServiceRepository.findById(reservation.getServiceId())
				.orElseThrow(() -> new ResourceNotFoundException("Service not found"));

		Provider provider = providerRepository.findByIdForUpdate(reservation.getProviderId())
				.orElseThrow(() -> new ResourceNotFoundException("Provider not found"));

		TimeRange slot = availabilityService.freeSlots(service, request.date(), now, reservation.getId()).stream()
				.filter(free -> free.start().equals(request.startTime()))
				.findFirst()
				.orElseThrow(() -> new ConflictException("The requested time slot is not available"));
		boolean clientBusy = reservationRepository.findConfirmedOverlappingForClient(reservation.getClientId(),
				request.date(), slot.start(), slot.end()).stream()
				.anyMatch(other -> !other.getId().equals(reservation.getId()));
		if (clientBusy) {
			throw new ConflictException("The client already has a reservation at that time");
		}

		ReservationHistory history = new ReservationHistory();
		history.setReservationId(reservation.getId());
		history.setAction(ReservationAction.RESCHEDULED);
		history.setActorUserId(actor.userId());
		history.setOldDate(reservation.getReservationDate());
		history.setOldStart(reservation.getStartTime());
		history.setOldEnd(reservation.getEndTime());

		reservation.setReservationDate(request.date());
		reservation.setStartTime(slot.start());
		reservation.setEndTime(slot.end());
		Reservation saved = reservationRepository.save(reservation);

		history.setNewDate(saved.getReservationDate());
		history.setNewStart(saved.getStartTime());
		history.setNewEnd(saved.getEndTime());
		historyRepository.save(history);

		auditLogger.info("RESERVATION_RESCHEDULED", "reservationId=" + saved.getId() + " actorId=" + actor.userId()
				+ " from=" + history.getOldDate() + "T" + history.getOldStart() + " to=" + saved.getReservationDate()
				+ "T" + saved.getStartTime());
		return reservationMapper.toResponse(saved, service, provider);
	}

	/**
	 * Reservas de cualquier estado entre dos fechas (máximo {@value #MAX_RANGE_DAYS} días): las propias para el
	 * cliente, la agenda completa para el proveedor.
	 */
	@Transactional(readOnly = true)
	public List<ReservationResponseDTO> listMine(LocalDate from, LocalDate to) {
		if (from.isAfter(to)) {
			throw badRequest("'from' must not be after 'to'");
		}
		if (from.plusDays(MAX_RANGE_DAYS - 1).isBefore(to)) {
			throw badRequest("The range cannot exceed " + MAX_RANGE_DAYS + " days");
		}
		AuthenticatedUser actor = SecurityUtils.requireCurrentUser();
		List<Reservation> reservations;
		if (actor.hasRole(Role.PROVIDER)) {
			Provider provider = providerRepository.findByUserId(actor.userId())
					.orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
			reservations = reservationRepository
					.findByProviderIdAndReservationDateBetweenOrderByReservationDateAscStartTimeAsc(provider.getId(),
							from, to);
		} else {
			reservations = reservationRepository
					.findByClientIdAndReservationDateBetweenOrderByReservationDateAscStartTimeAsc(actor.userId(), from,
							to);
		}
		Map<UUID, CatalogService> services = catalogServiceRepository
				.findAllById(reservations.stream().map(Reservation::getServiceId).distinct().toList()).stream()
				.collect(Collectors.toMap(CatalogService::getId, Function.identity()));
		Map<UUID, Provider> providers = providerRepository
				.findAllById(reservations.stream().map(Reservation::getProviderId).distinct().toList()).stream()
				.collect(Collectors.toMap(Provider::getId, Function.identity()));
		return reservations.stream()
				.map(r -> reservationMapper.toResponse(r, services.get(r.getServiceId()), providers.get(r.getProviderId())))
				.toList();
	}

	/**
	 * El rol PROVIDER actúa sobre las reservas de su agenda; cualquier otro, sobre las suyas como cliente.
	 * Devuelve si actúa como proveedor.
	 */
	private boolean requireOwner(AuthenticatedUser actor, Reservation reservation) {
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
