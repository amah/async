package org.async4j.foreach.parallel;

/**
 * Special callback that expected a boolean that indicates whether parallel tasks are still running on normal or abnormal completion of a iteration task.
 * This information is used internally to determine in end of a parallel loop.
 * @author Amah AHITE
 */
public interface IterationCallback {
	public void completed(boolean running);
	public void error(boolean running, Throwable e);
}
