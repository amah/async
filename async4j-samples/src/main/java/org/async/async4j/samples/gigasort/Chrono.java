package org.async.async4j.samples.gigasort;

public class Chrono {
	private final long start = System.currentTimeMillis();
	
	public long end(){
		return System.currentTimeMillis() - start;
	}
}
