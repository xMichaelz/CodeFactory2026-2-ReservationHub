package co.udea.codefactory.reservahub.report.service;

import co.udea.codefactory.reservahub.provider.entity.Provider;
import co.udea.codefactory.reservahub.provider.repository.ProviderRepository;
import co.udea.codefactory.reservahub.report.ReservationLifecycle;
import co.udea.codefactory.reservahub.report.dto.CancellationReport;
import co.udea.codefactory.reservahub.report.dto.DemandReport;
import co.udea.codefactory.reservahub.report.dto.OccupancyReport;
import co.udea.codefactory.reservahub.report.dto.ReservationHistoryItem;
import co.udea.codefactory.reservahub.reservation.entity.Reservation;
import co.udea.codefactory.reservahub.reservation.entity.ReservationStatus;
import co.udea.codefactory.reservahub.reservation.repository.ReservationRepository;
import co.udea.codefactory.reservahub.reservation.service.AvailabilityService;
import co.udea.codefactory.reservahub.reservation.service.AvailabilityService.SlotLoad;
import co.udea.codefactory.reservahub.security.SecurityUtils;
import co.udea.codefactory.reservahub.security.SecurityUtils.AuthenticatedUser;
import co.udea.codefactory.reservahub.servicecatalog.entity.CatalogService;
import co.udea.codefactory.reservahub.servicecatalog.repository.CatalogServiceRepository;
import co.udea.codefactory.reservahub.shared.enums.Role;
import co.udea.codefactory.reservahub.shared.exception.BusinessException;
import co.udea.codefactory.reservahub.shared.exception.ErrorCodes;
import co.udea.codefactory.reservahub.shared.exception.ForbiddenException;
import co.udea.codefactory.reservahub.shared.exception.ResourceNotFoundException;
import co.udea.codefactory.reservahub.user.entity.User;
import co.udea.codefactory.reservahub.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportService {

	static final int MAX_RANGE_DAYS = 31;

	private final ReservationRepository reservationRepository;
	private final CatalogServiceRepository catalogServiceRepository;
	private final ProviderRepository providerRepository;
	private final UserRepository userRepository;
	private final AvailabilityService availabilityService;

	public ReportService(ReservationRepository reservationRepository,
			CatalogServiceRepository catalogServiceRepository, ProviderRepository providerRepository,
			UserRepository userRepository, AvailabilityService availabilityService) {
		this.reservationRepository = reservationRepository;
		this.catalogServiceRepository = catalogServiceRepository;
		this.providerRepository = providerRepository;
		this.userRepository = userRepository;
		this.availabilityService = availabilityService;
	}

	/** HU-17. Incluye pasadas y futuras, con el ciclo de vida derivado. */
	@Transactional(readOnly = true)
	public List<ReservationHistoryItem> myHistory(LocalDate from, LocalDate to) {
		requireRange(from, to);
		AuthenticatedUser actor = requireRole(Role.CLIENT, "Only a client can read their own reservation history");
		return toHistory(reservationRepository
				.findByClientIdAndReservationDateBetweenOrderByReservationDateAscStartTimeAsc(actor.userId(), from,
						to));
	}

	/** HU-18. Todas las reservas del negocio, también canceladas y ya terminadas. */
	@Transactional(readOnly = true)
	public List<ReservationHistoryItem> businessHistory(LocalDate from, LocalDate to) {
		requireRange(from, to);
		Provider provider = requireProvider();
		return toHistory(reservationRepository
				.findByProviderIdAndReservationDateBetweenOrderByReservationDateAscStartTimeAsc(provider.getId(), from,
						to));
	}

	/** HU-19. Proporción de huecos ocupados frente a los que la agenda ofrecía. */
	@Transactional(readOnly = true)
	public OccupancyReport occupancy(LocalDate from, LocalDate to, UUID serviceId) {
		requireRange(from, to);
		Provider provider = requireProvider();
		List<CatalogService> services = servicesOf(provider.getId(), serviceId);
		int offered = 0;
		int occupied = 0;
		int free = 0;
		List<OccupancyReport.ServiceRow> byService = new ArrayList<>();
		Map<LocalDate, int[]> byDate = new HashMap<>();
		for (CatalogService service : services) {
			int serviceOffered = 0;
			int serviceOccupied = 0;
			int serviceFree = 0;
			for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
				SlotLoad load = availabilityService.slotLoad(service, date);
				serviceOffered += load.offered();
				serviceOccupied += load.occupied();
				serviceFree += load.free();
				int[] day = byDate.computeIfAbsent(date, ignored -> new int[3]);
				day[0] += load.offered();
				day[1] += load.occupied();
				day[2] += load.free();
			}
			offered += serviceOffered;
			occupied += serviceOccupied;
			free += serviceFree;
			byService.add(new OccupancyReport.ServiceRow(service.getId(), service.getName(), serviceOffered,
					serviceOccupied, serviceFree, rate(serviceOccupied, serviceOffered)));
		}
		List<OccupancyReport.DayRow> days = byDate.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.map(entry -> new OccupancyReport.DayRow(entry.getKey(), entry.getValue()[0], entry.getValue()[1],
						entry.getValue()[2], rate(entry.getValue()[1], entry.getValue()[0])))
				.toList();
		return new OccupancyReport(from, to, offered, occupied, free, rate(occupied, offered), byService, days);
	}

	/** HU-20. Reservas confirmadas por servicio. Las canceladas no cuentan. */
	@Transactional(readOnly = true)
	public DemandReport demand(LocalDate from, LocalDate to) {
		requireRange(from, to);
		Provider provider = requireProvider();
		Map<UUID, Long> delivered = new HashMap<>();
		for (Reservation reservation : reservationsOf(provider.getId(), from, to)) {
			if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
				delivered.merge(reservation.getServiceId(), 1L, Long::sum);
			}
		}
		List<DemandReport.ServiceDemand> byService = catalogServiceRepository
				.findByProviderIdOrderByNameAsc(provider.getId()).stream()
				.map(service -> new DemandReport.ServiceDemand(service.getId(), service.getName(),
						delivered.getOrDefault(service.getId(), 0L)))
				.sorted(Comparator.comparing(DemandReport.ServiceDemand::deliveredCount).reversed()
						.thenComparing(DemandReport.ServiceDemand::serviceName))
				.toList();
		long total = byService.stream().mapToLong(DemandReport.ServiceDemand::deliveredCount).sum();
		return new DemandReport(from, to, total, byService);
	}

	/** HU-21. Volumen de cancelaciones, distinguido por quién canceló. */
	@Transactional(readOnly = true)
	public CancellationReport cancellations(LocalDate from, LocalDate to, UUID serviceId) {
		requireRange(from, to);
		Provider provider = requireProvider();
		List<CatalogService> services = servicesOf(provider.getId(), serviceId);
		Map<UUID, long[]> counts = new HashMap<>();
		for (Reservation reservation : reservationsOf(provider.getId(), from, to)) {
			if (serviceId != null && !serviceId.equals(reservation.getServiceId())) {
				continue;
			}
			long[] row = counts.computeIfAbsent(reservation.getServiceId(), ignored -> new long[2]);
			if (reservation.getStatus() == ReservationStatus.CANCELLED_BY_CLIENT) {
				row[0]++;
			} else if (reservation.getStatus() == ReservationStatus.CANCELLED_BY_PROVIDER) {
				row[1]++;
			}
		}
		long byClient = 0;
		long byProvider = 0;
		List<CancellationReport.ServiceCancellations> byService = new ArrayList<>();
		for (CatalogService service : services) {
			long[] row = counts.getOrDefault(service.getId(), new long[2]);
			byClient += row[0];
			byProvider += row[1];
			byService.add(new CancellationReport.ServiceCancellations(service.getId(), service.getName(), row[0],
					row[1]));
		}
		return new CancellationReport(from, to, byClient, byProvider, byClient + byProvider, byService);
	}

	static BigDecimal rate(int occupied, int offered) {
		if (offered <= 0) {
			return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
		}
		return BigDecimal.valueOf(occupied).divide(BigDecimal.valueOf(offered), 4, RoundingMode.HALF_UP);
	}

	private List<ReservationHistoryItem> toHistory(List<Reservation> reservations) {
		LocalDateTime now = availabilityService.now();
		Map<UUID, CatalogService> services = catalogServiceRepository
				.findAllById(reservations.stream().map(Reservation::getServiceId).distinct().toList()).stream()
				.collect(Collectors.toMap(CatalogService::getId, Function.identity()));
		Map<UUID, Provider> providers = providerRepository
				.findAllById(reservations.stream().map(Reservation::getProviderId).distinct().toList()).stream()
				.collect(Collectors.toMap(Provider::getId, Function.identity()));
		Map<UUID, User> clients = userRepository
				.findAllById(reservations.stream().map(Reservation::getClientId).distinct().toList()).stream()
				.collect(Collectors.toMap(User::getId, Function.identity()));
		return reservations.stream().map(reservation -> {
			CatalogService service = services.get(reservation.getServiceId());
			Provider provider = providers.get(reservation.getProviderId());
			return new ReservationHistoryItem(reservation.getId(), reservation.getServiceId(),
					service == null ? "" : service.getName(), reservation.getProviderId(),
					provider == null ? "" : provider.getBusinessName(), reservation.getClientId(),
					displayName(clients.get(reservation.getClientId())), reservation.getReservationDate(),
					reservation.getStartTime(), reservation.getEndTime(), reservation.getStatus(),
					ReservationLifecycle.of(reservation, now));
		}).toList();
	}

	private List<Reservation> reservationsOf(UUID providerId, LocalDate from, LocalDate to) {
		return reservationRepository
				.findByProviderIdAndReservationDateBetweenOrderByReservationDateAscStartTimeAsc(providerId, from, to);
	}

	private List<CatalogService> servicesOf(UUID providerId, UUID serviceId) {
		if (serviceId == null) {
			return catalogServiceRepository.findByProviderIdOrderByNameAsc(providerId);
		}
		return List.of(catalogServiceRepository.findByIdAndProviderId(serviceId, providerId)
				.orElseThrow(() -> new ResourceNotFoundException("Service not found")));
	}

	private Provider requireProvider() {
		AuthenticatedUser actor = requireRole(Role.PROVIDER, "Only a provider can read this report");
		return providerRepository.findByUserId(actor.userId())
				.orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
	}

	private static AuthenticatedUser requireRole(Role role, String message) {
		AuthenticatedUser actor = SecurityUtils.requireCurrentUser();
		if (!actor.hasRole(role)) {
			throw new ForbiddenException(message);
		}
		return actor;
	}

	private static String displayName(User user) {
		if (user == null) {
			return "";
		}
		return (user.getFirstName() + " " + user.getLastName()).trim();
	}

	private static void requireRange(LocalDate from, LocalDate to) {
		if (from.isAfter(to)) {
			throw badRequest("'from' must not be after 'to'");
		}
		if (from.plusDays(MAX_RANGE_DAYS - 1L).isBefore(to)) {
			throw badRequest("The range cannot exceed " + MAX_RANGE_DAYS + " days");
		}
	}

	private static BusinessException badRequest(String message) {
		return new BusinessException(ErrorCodes.BAD_REQUEST, message, HttpStatus.BAD_REQUEST.value());
	}
}
