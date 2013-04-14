package org.async4j;
/**
 * Interface that define asynchronous method template. Implementor of {@link #run(Callback, Object)} method have to 
 * conform to contract defined on {@link Callback} interface.
 * @author Amah AHITE
 *
 * @param <P> The asynchronous operation input parameter type
 * @param <R> The asynchronous operation output value type
 */
public interface Task<P, R> {
	/**
	 * The asynchronous method template .
	 * @param k the callback object to be called on normal or abnormal completion 
	 * @param p the parameter of the method
	 */
	public void run(Callback<R> k, P p);
}
