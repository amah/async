package org.async4j.foreach.parallel;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.async4j.Callback;
import org.async4j.Task;

/**
 * Flow controller that limit the number of tasks that run simultaneous in parallel way 
 * @author Amah AHITE
 *
 */
public class BoundFlowController<E> implements FlowController<E>{
	private final Callback<Void> parent;
	private final Callback<Void> iterationCallback = new IterationCallback();
	private final AtomicLong runningCount = new AtomicLong();
	private final long maxParallel;
	private final ReentrantLock lock = new ReentrantLock();
	
	private volatile Task<E, Void> pendingTask;
	private volatile E pendingItem;
	private volatile Callback<Void> pendingCallback;
	
	private volatile Throwable error;
	
	public BoundFlowController(Callback<Void> parent, long maxParallel) {
		this.parent = parent;
		this.maxParallel = maxParallel;
	}

	public void run(Callback<Void> k, Task<E, Void> iterationTask, E item) {
		if(runningCount.incrementAndGet() <= maxParallel){
			iterationTask.run(iterationCallback, item);
			k.completed(null);
		}
		else{
			boolean wait;

			lock.lock();
			try {
				if (runningCount.get() > maxParallel) {
					this.pendingTask = iterationTask;
					this.pendingCallback = k;
					this.pendingItem = item;
					wait = true;
				} else {
					wait = false;
				}
			} finally {
				lock.unlock();
			}

			if(!wait){
				iterationTask.run(iterationCallback, item);
				k.completed(null);
			}
			else{
				if(runningCount.get() == 1){
					lock.lock();
					try {
						if (runningCount.get() == 1) {
							resume();
						}
					} finally {
						lock.unlock();
					}
				}
			}
		}
	}
	
	public boolean isRunning(){
		return runningCount.get() > 0;
	}

	protected class IterationCallback implements Callback<Void>{
		public void completed(Void result) {
			// TODO catch throwable
			long remaining = runningCount.decrementAndGet();
			if(remaining == maxParallel){
				resume();
			}
             			
			parent.completed(result);
		}

		public void error(Throwable e) {
			// TODO catch throwable
			error = e;
			long remaining = runningCount.decrementAndGet();
			if(remaining == maxParallel){
				resume();
			}

			parent.error(e);
		}
	}

	protected void resume(){
		if(pendingTask != null){
			Task<E, Void> resumeTask = null;
			E resumeItem = null;
			Callback<Void> resumeCallback = null;

			lock.lock();
			try {
				if (pendingTask != null) {
					resumeTask = pendingTask;
					pendingTask = null;
					
					resumeItem = pendingItem;
					pendingItem = null;
					
					resumeCallback = pendingCallback;
					pendingCallback = null;
				}
			} finally {
				lock.unlock();
			}
			
			if(resumeTask != null){
				if(error == null){
					resumeTask.run(iterationCallback, resumeItem);
					resumeCallback.completed(null);
				}
				else{
					// TODO Catch exception
					runningCount.decrementAndGet();
					resumeCallback.error(error);
				}
			}
		}
	}


}
