/*******************************************************************************
 * Copyright 2013 Async4j Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.async4j.flow;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.async4j.Callback;
import org.async4j.Task;

/**
 * Flow controller that limit the number of tasks that run simultaneous in
 * parallel way
 * 
 * @author Amah AHITE
 * 
 */
public class MultiEmiterFlowController<E> implements FlowController<E> {
	private final Callback<Void> parent;
	private final Callback<Void> iterationCallback = new IterationCallback();
	private final AtomicLong runningCount = new AtomicLong();
	private final long maxParallel;

	private final ConcurrentLinkedQueue<Iter<E>> pendings = new ConcurrentLinkedQueue<Iter<E>>();

	private volatile Throwable error;

	public MultiEmiterFlowController(Callback<Void> parent, long maxParallel) {
		this.parent = parent;
		this.maxParallel = maxParallel;
	}

	public void run(Callback<Void> k, Task<E, Void> iterationTask, E item) {
		if (runningCount.incrementAndGet() <= maxParallel) {
			iterationTask.run(iterationCallback, item);
			k.completed(null);
		} else {
			pendings.add(new Iter<E>(iterationTask, k, item));
		}
	}

	public boolean isRunning() {
		return runningCount.get() > 0;
	}

	protected class IterationCallback implements Callback<Void> {
		public void completed(Void result) {
			// TODO catch throwable
			long remaining = runningCount.decrementAndGet();
			if (remaining == maxParallel) {
				resume();
			}

			parent.completed(result);
		}

		public void error(Throwable e) {
			// TODO catch throwable
			error = e;
			long remaining = runningCount.decrementAndGet();
			if (remaining == maxParallel) {
				resume();
			}

			parent.error(e);
		}
	}

	protected void resume() {
		Iter<E> iter;
		while ((iter = pendings.poll()) == null) {}

		do{
			if (error == null) {
				iter.getTask().run(iterationCallback, iter.getItem());
				iter.getK().completed(null);
			} else {
				// TODO Catch exception
				runningCount.decrementAndGet();
				iter.getK().error(error);
			}
		}while(runningCount.get() <= maxParallel && (iter = pendings.poll()) != null);
	}

}
