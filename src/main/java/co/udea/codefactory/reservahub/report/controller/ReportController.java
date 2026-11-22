package co.udea.codefactory.reservahub.report.controller;

import co.udea.codefactory.reservahub.report.dto.CancellationReport;
import co.udea.codefactory.reservahub.report.dto.DemandReport;
import co.udea.codefactory.reservahub.report.dto.OccupancyReport;
import co.udea.codefactory.reservahub.report.dto.ReservationHistoryItem;
import co.udea.codefactory.reservahub.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports", description = "Reservation history and provider reports (HU-17 to HU-21)")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

	private final ReportService reportService;

	public ReportController(ReportService reportService) {
		this.reportService = reportService;
	}

	@GetMapping("/my-reservations")
	@Operation(summary = "HU-17. Own reservations, past and future, with a derived lifecycle")
	public List<ReservationHistoryItem> myReservations(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return reportService.myHistory(from, to);
	}

	@GetMapping("/business-reservations")
	@Operation(summary = "HU-18. Every reservation of the business, including cancelled and completed")
	public List<ReservationHistoryItem> businessReservations(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return reportService.businessHistory(from, to);
	}

	@GetMapping("/occupancy")
	@Operation(summary = "HU-19. Occupied slots over the slots the agenda offered")
	public OccupancyReport occupancy(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) UUID serviceId) {
		return reportService.occupancy(from, to, serviceId);
	}

	@GetMapping("/demand")
	@Operation(summary = "HU-20. Confirmed reservations per service. Cancelled ones are excluded")
	public DemandReport demand(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return reportService.demand(from, to);
	}

	@GetMapping("/cancellations")
	@Operation(summary = "HU-21. Cancellations split by client and provider")
	public CancellationReport cancellations(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) UUID serviceId) {
		return reportService.cancellations(from, to, serviceId);
	}
}
