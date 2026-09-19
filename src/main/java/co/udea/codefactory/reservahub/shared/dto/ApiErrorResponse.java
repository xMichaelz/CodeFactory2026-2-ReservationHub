package co.udea.codefactory.reservahub.shared.dto;

import java.util.List;

public record ApiErrorResponse(
		String errorCode,
		String message,
		List<String> details,
		String traceId
) {
	public ApiErrorResponse(String errorCode, String message, String traceId) {
		this(errorCode, message, List.of(), traceId);
	}
}
