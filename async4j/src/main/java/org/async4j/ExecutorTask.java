package org.async4j;

import java.util.concurrent.Executor;

/**
 * A {@link Task} implementation that delegate call to an embedded task using an {@link Executor}.  
 * @author Amah
 *
 */
public class ExecutorTask<P, R> implements Task<P, R>{
	private final Executor executor;
	private final Task<P, R> task;
	
	public ExecutorTask(Executor executor, Task<P, R> task) {
		super();
		this.executor = executor;
		this.task = task;
	}

	public void run(final Callback<? super R> k, final P p) {
		try {
			executor.execute(new Runnable() {
				public void run() {
					try {
						task.run(k, p);
					} catch (Throwable e) {
						k.error(e);
					}
				}
			});
		} catch (Throwable e) {
			k.error(e);
		}

	}
}
