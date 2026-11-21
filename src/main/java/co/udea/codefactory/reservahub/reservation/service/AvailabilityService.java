package co.udea.codefactory.reservahub.reservation.service;

import co.udea.codefactory.reservahub.reservation.DTO.AvailabilityResponseDTO;
import co.udea.codefactory.reservahub.reservation.entity.Reservation;
import co.udea.codefactory.reservahub.reservation.entity.ReservationStatus;
import co.udea.codefactory.reservahub.reservation.repository.ReservationRepository;
import co.udea.codefactory.reservahub.reservation.service.AvailabilityCalculator.AvailabilityQuery;
import co.udea.codefactory.reservahub.reservation.service.AvailabilityCalculator.BookedSlot;
import co.udea.codefactory.reservahub.reservation.service.AvailabilityCalculator.RequiredResource;
import co.udea.codefactory.reservahub.resource.entity.Resource;
import co.udea.codefactory.reservahub.resource.repository.ResourceRepository;
import co.udea.codefactory.reservahub.schedule.entity.AttentionSchedule;
import co.udea.codefactory.reservahub.schedule.repository.AttentionScheduleRepository;
import co.udea.codefactory.reservahub.schedule.repository.ScheduleBlockRepository;
import co.udea.codefactory.reservahub.servicecatalog.entity.CatalogService;
import co.udea.codefactory.reservahub.servicecatalog.entity.ServiceResource;
import co.udea.codefactory.reservahub.servicecatalog.repository.CatalogServiceRepository;
import co.udea.codefactory.reservahub.servicecatalog.repository.ServiceResourceRepository;
import co.udea.codefactory.reservahub.shared.enums.EntityStatus;
import co.udea.codefactory.reservahub.shared.exception.BusinessException;
import co.udea.codefactory.reservahub.shared.exception.ErrorCodes;
import co.udea.codefactory.reservahub.shared.exception.ResourceNotFoundException;
import co.udea.codefactory.reservahub.shared.time.TimeRange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AvailabilityService {

	static final int MAX_RANGE_DAYS = 31;

	private final CatalogServiceRepository catalogServiceRepository;
	private final AttentionScheduleRepository scheduleRepository;
	private final ScheduleBlockRepository blockRepository;
	private final ReservationRepository reservationRepository;
	private final ServiceResourceRepository serviceResourceRepository;
	private final ResourceRepository resourceRepository;
	private final ZoneId businessZone;

	public AvailabilityService(CatalogServiceRepository catalogServiceRepository,
			AttentionScheduleRepository scheduleRepository, ScheduleBlockRepository blockRepository,
			ReservationRepository reservationRepository, ServiceResourceRepository serviceResourceRepository,
			ResourceRepository resourceRepository, @Value("${app.business.zone:America/Bogota}") String zone) {
		this.catalogServiceRepository = catalogServiceRepository;
		this.scheduleRepository = scheduleRepository;
		this.blockRepository = blockRepository;
		this.reservationRepository = reservationRepository;
		this.serviceResourceRepository = serviceResourceRepository;
		this.resourceRepository = resourceRepository;
		this.businessZone = ZoneId.of(zone);
	}

	@Transactional(readOnly = true)
	public AvailabilityResponseDTO getAvailability(UUID serviceId, LocalDate from, LocalDate to) {
		if (from.isAfter(to)) {
			throw badRequest("'from' must not be after 'to'");
		}
		if (from.plusDays(MAX_RANGE_DAYS - 1L).isBefore(to)) {
			throw badRequest("The range cannot exceed " + MAX_RANGE_DAYS + " days");
		}
		CatalogService service = catalogServiceRepository.findById(serviceId)
				.orElseThrow(() -> new ResourceNotFoundException("Service not found"));

		LocalDateTime now = now();
		List<RequiredResource> required = requiredResources(serviceId);
		Map<UUID, Set<UUID>> resourcesByService = new HashMap<>();
		resourcesByService.put(serviceId, idsOf(required));

		List<AvailabilityResponseDTO.Day> days = new ArrayList<>();
		for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
			List<TimeRange> free = freeSlots(service, date, now, required, resourcesByService, null);
			if (!free.isEmpty()) {
				days.add(new AvailabilityResponseDTO.Day(date, free.stream()
						.map(r -> new AvailabilityResponseDTO.Slot(r.start(), r.end())).toList()));
			}
		}
		return new AvailabilityResponseDTO(serviceId, days);
	}

	/** Hora actual en la zona del negocio. */
	public LocalDateTime now() {
		return LocalDateTime.now(businessZone);
	}

	/**
	 * Huecos libres del servicio en un día, leyendo la agenda y las reservas confirmadas en el momento de la
	 * llamada. Quien reserva la llama con el proveedor bloqueado para que el resultado no cambie antes de guardar.
	 */
	public List<TimeRange> freeSlots(CatalogService service, LocalDate date, LocalDateTime now) {
		return freeSlots(service, date, now, null);
	}

	/**
	 * Igual que {@link #freeSlots(CatalogService, LocalDate, LocalDateTime)} pero sin contar la reserva indicada:
	 * al reprogramar, la reserva no compite consigo misma por el nuevo horario (HU-16).
	 */
	public List<TimeRange> freeSlots(CatalogService service, LocalDate date, LocalDateTime now,
			UUID ignoredReservationId) {
		List<RequiredResource> required = requiredResources(service.getId());
		Map<UUID, Set<UUID>> resourcesByService = new HashMap<>();
		resourcesByService.put(service.getId(), idsOf(required));
		return freeSlots(service, date, now, required, resourcesByService, ignoredReservationId);
	}

	/**
	 * Huecos que la agenda ofrece ese día (sin reservas) y los que siguen libres. Cuenta también fechas pasadas
	 * y huecos ya iniciados: la ocupación histórica no usa el recorte de la disponibilidad en vivo.
	 */
	public SlotLoad slotLoad(CatalogService service, LocalDate date) {
		List<RequiredResource> required = requiredResources(service.getId());
		Map<UUID, Set<UUID>> resourcesByService = new HashMap<>();
		resourcesByService.put(service.getId(), idsOf(required));
		LocalDateTime anchor = date.atStartOfDay();
		int offered = AvailabilityCalculator
				.calculate(query(service, date, anchor, required, resourcesByService, null, true, true).build())
				.size();
		int free = AvailabilityCalculator
				.calculate(query(service, date, anchor, required, resourcesByService, null, true, false).build())
				.size();
		return new SlotLoad(offered, free);
	}

	/** {@code offered} es la grilla de la agenda; {@code free} lo que sigue libre. Ocupados = offered − free. */
	public record SlotLoad(int offered, int free) {
		public int occupied() {
			return Math.max(0, offered - free);
		}
	}

	private List<TimeRange> freeSlots(CatalogService service, LocalDate date, LocalDateTime now,
			List<RequiredResource> required, Map<UUID, Set<UUID>> resourcesByService, UUID ignoredReservationId) {
		return AvailabilityCalculator.calculate(
				query(service, date, now, required, resourcesByService, ignoredReservationId, false, false).build());
	}

	private AvailabilityQuery.Builder query(CatalogService service, LocalDate date, LocalDateTime now,
			List<RequiredResource> required, Map<UUID, Set<UUID>> resourcesByService, UUID ignoredReservationId,
			boolean includePast, boolean ignoreBookings) {
		List<AttentionSchedule> schedules = scheduleRepository.findByProviderIdAndDayOfWeek(service.getProviderId(),
				co.udea.codefactory.reservahub.shared.enums.DayOfWeek.valueOf(date.getDayOfWeek().name()));
		List<TimeRange> windows = schedules.stream()
				.map(s -> new TimeRange(s.getStartTime(), s.getEndTime())).toList();
		List<TimeRange> blocks = schedules.stream()
				.flatMap(s -> blockRepository.findByScheduleIdAndBlockDate(s.getId(), date).stream())
				.map(b -> new TimeRange(b.getStartTime(), b.getEndTime())).toList();
		List<BookedSlot> bookings = ignoreBookings ? List.of()
				: bookingsOf(service.getProviderId(), date, resourcesByService, ignoredReservationId);
		boolean active = includePast || service.getStatus() == EntityStatus.ACTIVE;
		return AvailabilityQuery.builder().date(date).now(now).serviceActive(active)
				.durationMinutes(service.getDurationMinutes()).windows(windows).blocks(blocks)
				.bookings(bookings).resources(required).includePast(includePast);
	}

	private List<RequiredResource> requiredResources(UUID serviceId) {
		Set<UUID> ids = idsOf(serviceResourceRepository.findByIdServiceId(serviceId));
		return resourceRepository.findAllById(ids).stream()
				.filter(r -> r.getStatus() == EntityStatus.ACTIVE)
				.map((Resource r) -> new RequiredResource(r.getId(), r.getQuantity())).toList();
	}

	private List<BookedSlot> bookingsOf(UUID providerId, LocalDate date, Map<UUID, Set<UUID>> cache,
			UUID ignoredReservationId) {
		List<Reservation> reservations = reservationRepository.findByProviderIdAndReservationDateAndStatus(
				providerId, date, ReservationStatus.CONFIRMED).stream()
				.filter(r -> !r.getId().equals(ignoredReservationId)).toList();
		Set<UUID> missing = new HashSet<>();
		reservations.forEach(r -> {
			if (!cache.containsKey(r.getServiceId())) {
				missing.add(r.getServiceId());
			}
		});
		if (!missing.isEmpty()) {
			missing.forEach(id -> cache.put(id, new HashSet<>()));
			serviceResourceRepository.findByIdServiceIdIn(missing)
					.forEach(sr -> cache.get(sr.getId().serviceId()).add(sr.getId().resourceId()));
		}
		return reservations.stream().map(r -> new BookedSlot(new TimeRange(r.getStartTime(), r.getEndTime()),
				r.getStatus(), cache.get(r.getServiceId()))).toList();
	}

	private static Set<UUID> idsOf(List<?> items) {
		Set<UUID> ids = new HashSet<>();
		for (Object item : items) {
			if (item instanceof ServiceResource sr) {
				ids.add(sr.getId().resourceId());
			} else if (item instanceof RequiredResource rr) {
				ids.add(rr.resourceId());
			}
		}
		return ids;
	}

	private static BusinessException badRequest(String message) {
		return new BusinessException(ErrorCodes.BAD_REQUEST, message, HttpStatus.BAD_REQUEST.value());
	}
}
