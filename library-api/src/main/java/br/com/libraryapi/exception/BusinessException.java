package br.com.libraryapi.exception;

public class BusinessException extends RuntimeException {

	private static final long serialVersionUID = 5132317670132001676L;

	public BusinessException() {
		super();
	}

	public BusinessException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public BusinessException(String message, Throwable cause) {
		super(message, cause);
	}

	public BusinessException(String message) {
		super(message);
	}

	public BusinessException(Throwable cause) {
		super(cause);
	}

}
