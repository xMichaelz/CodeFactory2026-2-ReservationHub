package co.udea.codefactory.reservahub.shared.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends BusinessException {

	public ForbiddenException(String message) {
		super(ErrorCodes.FORBIDDEN, message, HttpStatus.FORBIDDEN.value());
	}
}
