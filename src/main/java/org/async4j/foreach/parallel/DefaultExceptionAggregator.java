package org.async4j.foreach.parallel;

/**
 * Exception aggregator that keep only the last exception observed.
 * @author Amah AHITE
 *
 */
public class DefaultExceptionAggregator implements ExceptionAggregator {
	private volatile Throwable exception;

	public void handle(Throwable e) {
		this.exception = e; 
	}

	public Throwable getAggregated() {
		return exception;
	}
	
}
