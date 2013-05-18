package org.async4j.foreach.parallel;

public interface ExceptionAggregator {
	public void handle(Throwable e);
	public Throwable getAggregated();
}
