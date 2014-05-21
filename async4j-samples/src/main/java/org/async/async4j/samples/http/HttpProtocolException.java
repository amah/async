package org.async.async4j.samples.http;

public class HttpProtocolException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public HttpProtocolException() {
		super();
	}

	public HttpProtocolException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public HttpProtocolException(String message, Throwable cause) {
		super(message, cause);
	}

	public HttpProtocolException(String message) {
		super(message);
	}

	public HttpProtocolException(Throwable cause) {
		super(cause);
	}

}
