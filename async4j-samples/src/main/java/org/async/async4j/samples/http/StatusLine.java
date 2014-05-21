package org.async.async4j.samples.http;

public class StatusLine {
	private final String httpVersion;
	private final int statusCode;
	private final String reasonPhrase;
	
	public StatusLine(String httpVersion, int statusCode, String reasonPhrase) {
		super();
		this.httpVersion = httpVersion;
		this.statusCode = statusCode;
		this.reasonPhrase = reasonPhrase;
	}
	
	public String getHttpVersion() {
		return httpVersion;
	}
	public int getStatusCode() {
		return statusCode;
	}
	public String getReasonPhrase() {
		return reasonPhrase;
	}
}