package co.udea.codefactory.reservahub.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Ocupación de la agenda (HU-19): huecos ocupados frente a los que la agenda ofrecía.
 * {@code occupancyRate} es occupied / offered, con 4 decimales. Si no hubo oferta, es 0.
 */
public record OccupancyReport(LocalDate from, LocalDate to, int offeredSlots, int occupiedSlots, int freeSlots,
		BigDecimal occupancyRate, List<ServiceRow> byService, List<DayRow> byDate) {

	public record ServiceRow(UUID serviceId, String serviceName, int offeredSlots, int occupiedSlots, int freeSlots,
			BigDecimal occupancyRate) {
	}

	public record DayRow(LocalDate date, int offeredSlots, int occupiedSlots, int freeSlots,
			BigDecimal occupancyRate) {
	}
}
