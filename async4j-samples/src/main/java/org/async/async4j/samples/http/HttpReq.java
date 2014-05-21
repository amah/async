package org.async.async4j.samples.http;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class HttpReq {
	private final String method;
	private final String uri;
	private final Map<String, String> headers;
	private final byte[] body;

	public HttpReq(String method, String uri, Map<String, String> headers) {
		this.method = method;
		this.uri = uri;
		this.headers = headers;
		this.body = null;
	}

	public static HttpReq get(URI uri){
		String rawQuery = uri.getRawQuery();
		String path = uri.getRawPath() + (rawQuery == null ? "" :  rawQuery);
		String host = uri.getHost();
		
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Host", host);
		
		return new HttpReq("GET", path, headers);
	}

	public String getMethod() {
		return method;
	}

	public String getUri() {
		return uri;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public byte[] getBody() {
		return body;
	}
}
