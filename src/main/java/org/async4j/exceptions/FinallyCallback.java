package org.async4j.exceptions;

import org.async4j.Callback;

/**
 * Callback that hold either a value or an exception that is propagated to the parent callback depending on the completion status of the asynchronous final task.
 * Rules are the following:
 * <ul>
 * <li> Exception held by this callback is null (which means the try or catch tasks does not report any exception)
 * <ul>
 * <li>If successful completion, the value held by this callback is propagated to the parent callback using {@link Callback#completed(Object)} method</li>
 * <li>Else the exception passed to {@link #error(Throwable)} is propagated to the parent callback the same way</li>
 * </ul>
 * </li>
 * <li> Exception held by this callback is not null (which means the try or catch tasks failed)
 * <ul>
 * <li>If successful completion, the exception held by this callback is propagated to the parent</li>
 * <li>Else the exception passed to {@link #error(Throwable)} is propagated to the parent callback the same way</li>
 * </ul>
 * </li>
 * </ul>
 * 
 * @author Amah AHITE
 *
 * @param <R> result type of this callback.
 */
public class FinallyCallback<R> implements Callback<Void> {
	private final Callback<R> parent;
	private final Throwable exception;
	private final R value;
	public FinallyCallback(Callback<R> parent, R value, Throwable exception) {
		this.value = value;
		this.parent = parent;
		this.exception = exception;
	}

	public void completed(Void result) {
		try {
			if(exception == null){
				parent.completed(value);
			}
			else{
				parent.error(exception);
			}
		} catch (Exception e) {
			parent.error(e);
		}
	}

	public void error(Throwable e) {
		parent.error(e);
	}
}
