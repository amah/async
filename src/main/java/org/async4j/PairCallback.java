package org.async4j;

/**
 * This interface is defined for asynchronous operations which need to return two values.
 * @author Amah AHITE
 */
public interface PairCallback<R1,R2> {
	public void completed(R1 r1, R2 r2);
	public void error(Throwable e);
}
