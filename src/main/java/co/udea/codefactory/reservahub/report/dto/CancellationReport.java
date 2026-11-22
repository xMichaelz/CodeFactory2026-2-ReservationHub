package co.udea.codefactory.reservahub.report.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Cancelaciones del negocio (HU-21), separadas por quién las hizo. */
public record CancellationReport(LocalDate from, LocalDate to, long cancelledByClient, long cancelledByProvider,
		long total, List<ServiceCancellations> byService) {

	public record ServiceCancellations(UUID serviceId, String serviceName, long cancelledByClient,
			long cancelledByProvider) {
	}
}
