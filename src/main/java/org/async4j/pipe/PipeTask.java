package org.async4j.pipe;

import org.async4j.Callback;
import org.async4j.Task;

/**
 * Pipe task chain two tasks using {@link PipeCallback}. The second task is invoked only when the first task successfully completes.
 * 
 * @author Amah AHITE
 *
 * @param <P> Input type of the first asynchronous task
 * @param <I> Output type of the first asynchronous task and input type of the second asynchronous task
 * @param <R> Output type of the second asynchronous type
 */
public class PipeTask<P,I,R> implements Task<P, R>{
	private Task<P, I> firstTask;
	private Task<I, R> nextTask;

	public PipeTask(Task<P, I> firstTask, Task<I, R> nextTask) {
		super();
		this.firstTask = firstTask;
		this.nextTask = nextTask;
	}

	public void run(Callback<R> callback, P p) {
		final PipeCallback<I, R> continueCallback;
		try{
			continueCallback = new PipeCallback<I, R>(nextTask, callback);
		}catch (Throwable e) {
			callback.error(e);
			return;
		}
		
		firstTask.run(continueCallback, p);
		return;
	}

}
