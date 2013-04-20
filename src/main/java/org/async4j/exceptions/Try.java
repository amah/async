package org.async4j.exceptions;

import org.async4j.Callback;
import org.async4j.Task;

/**
 * Task that implements asynchronous version of the try/catch/finally blocks.
 * @author Amah AHITE
 *
 * @param <P> asynchronous input parameter type
 * @param <R> asynchronous output parameter type
 */
public class Try<P, R> implements Task<P, R> {
	private final Task<P, R> task;
	private final Task<Throwable, R> catchTask;
	private final Task<Void, Void> finallyTask;

	
	public Try(Task<P, R> task, Task<Throwable, R> catchTask, Task<Void, Void> finallyTask) {
		super();
		this.task = task;
		this.catchTask = catchTask;
		this.finallyTask = finallyTask;
	}


	public void run(Callback<R> k, P p) {
		try{
			task.run(new TryCallback<R>(k, catchTask, finallyTask), p);
		}
		catch (Throwable e) {
			k.error(e);
		}
	}
}
