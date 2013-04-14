package org.async4j;


/**
 * The callback interface is passed to asynchronous operations which must conform to following rules.
 * <ol>
 * 	<li>Either method {@link #completed(Object)} or {@link #error(Object)} MUST be called to report completion of the asynchronous task</li>
 * 	<li>Asynchronous operation MUST not throw exception </li>
 * </ol>
 * @author Amah AHITE
 * @param <R> The returned result type
 */
public interface Callback<R> {
	/**
	 * This method is called by asynchronous operation to notify processing end along with the result.
	 * Implementor of this method MUST not throw any exception. 
	 * @param result asynchronous operation result 
	 */
	public void completed(R result);
	
	/**
	 * Method called to report exception.
	 * Implementor of this method MUST not throw any exception. 
	 * @param e exception to be reported
	 */
	public void error(Throwable e);
}
