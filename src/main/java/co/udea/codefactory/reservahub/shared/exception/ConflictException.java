package co.udea.codefactory.reservahub.shared.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends BusinessException {

	public ConflictException(String message) {
		super(ErrorCodes.CONFLICT, message, HttpStatus.CONFLICT.value());
	}
}
