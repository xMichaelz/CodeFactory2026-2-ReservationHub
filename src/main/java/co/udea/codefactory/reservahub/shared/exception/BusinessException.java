package co.udea.codefactory.reservahub.shared.exception;

public class BusinessException extends RuntimeException {

	private final String errorCode;
	private final int httpStatus;

	public BusinessException(String errorCode, String message, int httpStatus) {
		super(message);
		this.errorCode = errorCode;
		this.httpStatus = httpStatus;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public int getHttpStatus() {
		return httpStatus;
	}
}
