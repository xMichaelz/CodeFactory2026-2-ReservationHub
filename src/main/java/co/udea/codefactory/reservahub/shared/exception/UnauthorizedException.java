package co.udea.codefactory.reservahub.shared.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BusinessException {

	public UnauthorizedException(String message) {
		super(ErrorCodes.UNAUTHORIZED, message, HttpStatus.UNAUTHORIZED.value());
	}
}
