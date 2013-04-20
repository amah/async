package org.async4j.exceptions;

import org.async4j.Callback;
import org.async4j.Task;

/**
 * A callback to be passed to asynchronous task to manage exception handling
 * 
 * @author Amah AHITE
 * 
 * @param <R>
 */
public class TryCallback<R> implements Callback<R> {
	private final Callback<R> parent;
	private final Task<Throwable, R> catchTask;
	private final Task<Void, Void> finallyTask;

	public TryCallback(Callback<R> parent, Task<Throwable, R> catchTask, Task<Void, Void> finallyTask) {
		super();
		this.parent = parent;
		this.catchTask = catchTask;
		this.finallyTask = finallyTask;
	}

	public void completed(R result) {
		try {
			if(finallyTask != null){
				finallyTask.run(new FinallyCallback<R>(parent, result, null), null);
			}else{
				parent.completed(result);
			}
		} catch (Throwable e) {
			error(e);
		}
	}

	public void error(Throwable e) {
		try {
			if(catchTask != null){
				catchTask.run(new CatchCallback<R>(parent, finallyTask), e);
			}else{
				if(finallyTask != null){
					finallyTask.run(new FinallyCallback<R>(parent, null, e), null);
				}else{
					parent.error(e);
				}
			}
		} catch (Throwable ex) {
			parent.error(ex);
		}
	}

}
