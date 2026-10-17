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
import co.udea.codefactory.reservahub.shared.exception.ResourceNotFoundException;
import co.udea.codefactory.reservahub.shared.time.TimeRange;
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

	private static final String SERVICE_NOT_FOUND = "Service not found";
	private static final String PROVIDER_NOT_FOUND = "Provider not found";
	private static final String RESERVATION_ID = "reservationId=";
	private static final String ACTOR_ID = " actorId=";

	private final ReservationRepository reservationRepository;
	private final ReservationHistoryRepository historyRepository;
	private final CatalogServiceRepository catalogServiceRepository;
	private final ProviderRepository providerRepository;
	private final AvailabilityService availabilityService;
	private final ReservationMapper reservationMapper;
	private final AuditLogger auditLogger;
	private final ReservationAccessPolicy accessPolicy;

	public ReservationService(ReservationRepository reservationRepository,
			ReservationHistoryRepository historyRepository, CatalogServiceRepository catalogServiceRepository,
			ProviderRepository providerRepository, AvailabilityService availabilityService,
			ReservationMapper reservationMapper, AuditLogger auditLogger, ReservationAccessPolicy accessPolicy) {
		this.reservationRepository = reservationRepository;
		this.historyRepository = historyRepository;
		this.catalogServiceRepository = catalogServiceRepository;
		this.providerRepository = providerRepository;
		this.availabilityService = availabilityService;
		this.reservationMapper = reservationMapper;
		this.auditLogger = auditLogger;
		this.accessPolicy = accessPolicy;
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
				.orElseThrow(() -> new ResourceNotFoundException(SERVICE_NOT_FOUND));
		UUID clientId = accessPolicy.clientFor(actor, request, service);
		if (service.getStatus() != EntityStatus.ACTIVE) {
			throw badRequest("The service is not active");
		}
		LocalDateTime now = availabilityService.now();
		if (!request.date().atTime(request.startTime()).isAfter(now)) {
			throw badRequest("The reservation must start in the future");
		}

		Provider provider = providerRepository.findByIdForUpdate(service.getProviderId())
				.orElseThrow(() -> new ResourceNotFoundException(PROVIDER_NOT_FOUND));

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

		auditLogger.info("RESERVATION_CREATED", RESERVATION_ID + saved.getId() + " serviceId=" + service.getId()
				+ " clientId=" + clientId + ACTOR_ID + actor.userId() + " date=" + saved.getReservationDate()
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
		boolean byProvider = accessPolicy.requireOwner(actor, reservation);
		if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
			throw new ConflictException("The reservation is already cancelled");
		}
		if (!reservation.getReservationDate().atTime(reservation.getStartTime()).isAfter(availabilityService.now())) {
			throw badRequest("Only future reservations can be cancelled");
		}
		String reason = ReservationAccessPolicy.cancellationReason(byProvider, request == null ? null : request.reason());

		reservation.setStatus(byProvider ? ReservationStatus.CANCELLED_BY_PROVIDER
				: ReservationStatus.CANCELLED_BY_CLIENT);
		Reservation saved = reservationRepository.save(reservation);

		ReservationHistory history = new ReservationHistory();
		history.setReservationId(saved.getId());
		history.setAction(ReservationAction.CANCELLED);
		history.setActorUserId(actor.userId());
		history.setReason(reason);
		history.setOldDate(saved.getReservationDate());
		history.setOldStart(saved.getStartTime());
		history.setOldEnd(saved.getEndTime());
		historyRepository.save(history);

		auditLogger.info("RESERVATION_CANCELLED", RESERVATION_ID + saved.getId() + ACTOR_ID + actor.userId()
				+ " status=" + saved.getStatus());
		CatalogService service = catalogServiceRepository.findById(saved.getServiceId())
				.orElseThrow(() -> new ResourceNotFoundException(SERVICE_NOT_FOUND));
		Provider provider = providerRepository.findById(saved.getProviderId())
				.orElseThrow(() -> new ResourceNotFoundException(PROVIDER_NOT_FOUND));
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
		accessPolicy.requireOwner(actor, reservation);
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
				.orElseThrow(() -> new ResourceNotFoundException(SERVICE_NOT_FOUND));

		Provider provider = providerRepository.findByIdForUpdate(reservation.getProviderId())
				.orElseThrow(() -> new ResourceNotFoundException(PROVIDER_NOT_FOUND));

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

		auditLogger.info("RESERVATION_RESCHEDULED", RESERVATION_ID + saved.getId() + ACTOR_ID + actor.userId()
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
		if (from.plusDays(MAX_RANGE_DAYS - 1L).isBefore(to)) {
			throw badRequest("The range cannot exceed " + MAX_RANGE_DAYS + " days");
		}
		AuthenticatedUser actor = SecurityUtils.requireCurrentUser();
		List<Reservation> reservations;
		if (actor.hasRole(Role.PROVIDER)) {
			Provider provider = providerRepository.findByUserId(actor.userId())
					.orElseThrow(() -> new ResourceNotFoundException(PROVIDER_NOT_FOUND));
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

	private static BusinessException badRequest(String message) {
		return ReservationAccessPolicy.badRequest(message);
	}
}
