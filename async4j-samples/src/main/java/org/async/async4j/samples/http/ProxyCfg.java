package org.async.async4j.samples.http;

public class ProxyCfg {
	private final String host;
	private final int port;
	
	public ProxyCfg(String host, int port) {
		this.host = host;
		this.port = port;
	}
	public String getHost() {
		return host;
	}
	public int getPort() {
		return port;
	}
}
