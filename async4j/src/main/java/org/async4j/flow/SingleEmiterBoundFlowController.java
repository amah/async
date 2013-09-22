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

import java.util.concurrent.atomic.AtomicLong;

import org.async4j.Callback;
import org.async4j.FunctionAsync;

/**
 * Flow controller that limit the number of tasks that run simultaneous in
 * parallel way
 * 
 * @author Amah AHITE
 * 
 */
public class SingleEmiterBoundFlowController<E> implements FlowController<E> {
	private final Callback<Void> parent;
	private final Callback<Void> iterationCallback = new IterationCallback();
	private final AtomicLong runningCount = new AtomicLong();
	private final long maxParallel;

	private volatile boolean pending = false;
	private volatile FunctionAsync<E, Void> pendingTask;
	private volatile E pendingItem;
	private volatile Callback<Void> pendingCallback;

	private volatile Throwable error;

	public SingleEmiterBoundFlowController(Callback<Void> parent, long maxParallel) {
		this.parent = parent;
		this.maxParallel = maxParallel;
	}

	public void run(Callback<Void> k, FunctionAsync<E, Void> iterationTask, E item) {

		long seq = runningCount.incrementAndGet();
		if (seq <= maxParallel) {
			iterationTask.apply(iterationCallback, item);
			k.completed(null);
		} else {
			this.pendingTask = iterationTask;
			this.pendingCallback = k;
			this.pendingItem = item;
			this.pending = true;
		}
	}


	protected void resume() {
		while (! pending) {}
		
		FunctionAsync<E, Void> resumeTask = null;
		E resumeItem = null;
		Callback<Void> resumeCallback = null;

		resumeTask = pendingTask;
		resumeItem = pendingItem;
		resumeCallback = pendingCallback;
		pendingTask = null;
		pendingItem = null;
		pendingCallback = null;
		pending = false;
		
		if (error == null) {
			resumeTask.apply(iterationCallback, resumeItem);
			resumeCallback.completed(null);
		} else {
			// TODO Catch exception
			runningCount.decrementAndGet();
			resumeCallback.error(error);
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

}
