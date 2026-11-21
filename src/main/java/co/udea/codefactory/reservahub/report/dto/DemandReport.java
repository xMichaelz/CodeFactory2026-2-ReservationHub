package co.udea.codefactory.reservahub.report.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Demanda por servicio (HU-20). {@code deliveredCount} cuenta reservas confirmadas.
 * Las canceladas no entran: no son un servicio efectivamente prestado.
 */
public record DemandReport(LocalDate from, LocalDate to, long totalDelivered, List<ServiceDemand> byService) {

	public record ServiceDemand(UUID serviceId, String serviceName, long deliveredCount) {
	}
}
