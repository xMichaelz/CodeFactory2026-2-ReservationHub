package co.udea.codefactory.reservahub.shared.exception;

import co.udea.codefactory.reservahub.shared.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest request) {
		return ResponseEntity.status(ex.getHttpStatus())
				.body(new ApiErrorResponse(ex.getErrorCode(), ex.getMessage(), traceId(request)));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		List<String> details = ex.getBindingResult().getFieldErrors().stream()
				.map(this::formatFieldError)
				.toList();
		return ResponseEntity.badRequest()
				.body(new ApiErrorResponse(ErrorCodes.VALIDATION_ERROR, "Request validation failed", details,
						traceId(request)));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleConstraint(ConstraintViolationException ex,
			HttpServletRequest request) {
		List<String> details = ex.getConstraintViolations().stream()
				.map(v -> v.getPropertyPath() + ": " + v.getMessage())
				.toList();
		return ResponseEntity.badRequest()
				.body(new ApiErrorResponse(ErrorCodes.VALIDATION_ERROR, "Request validation failed", details,
						traceId(request)));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest()
				.body(new ApiErrorResponse(ErrorCodes.BAD_REQUEST, "Malformed request body", traceId(request)));
	}

	@ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
	public ResponseEntity<ApiErrorResponse> handleAuth(RuntimeException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(new ApiErrorResponse(ErrorCodes.UNAUTHORIZED, "Invalid credentials", traceId(request)));
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(new ApiErrorResponse(ErrorCodes.FORBIDDEN, "Access denied", traceId(request)));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
		log.error("Unhandled error [{}]", traceId(request), ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiErrorResponse(ErrorCodes.INTERNAL_ERROR, "Unexpected server error", traceId(request)));
	}

	private String formatFieldError(FieldError error) {
		return error.getField() + ": " + error.getDefaultMessage();
	}

	private String traceId(HttpServletRequest request) {
		Object existing = request.getAttribute("traceId");
		if (existing != null) {
			return existing.toString();
		}
		String generated = UUID.randomUUID().toString();
		request.setAttribute("traceId", generated);
		return generated;
	}
}
