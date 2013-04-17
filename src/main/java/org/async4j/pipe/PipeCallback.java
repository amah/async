package org.async4j.pipe;

import org.async4j.Callback;
import org.async4j.Task;

/**
 * The pipe callback is used internally by PipeTask to chain two asynchronous tasks. It is passed to the first task and on its completion pass asynchronously the 
 * output as input to the second task. When the first Task report an error (call to {@link #error(Throwable)} the second task is not called and the error is forwarded 
 * to the parent callback.  
 * @author Amah AHITE
 *
 * @param <P> The type of the first asynchronous task result which corresponds to the type of the second task input parameter
 * @param <R> The type of the second asynchronous task result 
 */
public class PipeCallback<P, R> implements Callback<P>{
	private Callback<R> parentCallback;
	private Task<P, R> nextTask;
	
	public PipeCallback(Task<P, R> nextTask, Callback<R> parentCallback) {
		super();
		this.nextTask = nextTask;
		this.parentCallback = parentCallback;
	}

	public void completed(P p) {
		nextTask.run(parentCallback, p);
	}

	public void error(Throwable e) {
		parentCallback.error(e);
	}

}
