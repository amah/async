package org.async4j.exceptions;

import org.async4j.Callback;
import org.async4j.Task;

/**
 * Callback that is passed to the asynchronous exception handler task.
 * 
 * @author Amah AHITE
 * @param <R>
 *            the result type
 */
public class CatchCallback<R> implements Callback<R> {
	private final Callback<R> parent;
	private final Task<Void, Void> finallyTask;

	public CatchCallback(Callback<R> parent, Task<Void, Void> finallyTask) {
		super();
		this.parent = parent;
		this.finallyTask = finallyTask;
	}

	public void completed(R result) {
		try {
			if(finallyTask != null){
				finallyTask.run(new FinallyCallback<R>(parent, result, null), null);
			}
		} catch (Throwable e) {
			parent.error(e);
		}
	}

	public void error(Throwable e) {
		try {
			if(finallyTask != null){
				finallyTask.run(new FinallyCallback<R>(parent, null, e), null);
			}
		} catch (Throwable ex) {
			parent.error(ex);
		}
	}

}
