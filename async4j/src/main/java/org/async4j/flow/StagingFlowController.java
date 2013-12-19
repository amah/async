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

import org.async4j.Callback;
import org.async4j.FunctionAsync;

/**
 * Flow controller that limit the number of tasks that run simultaneous in
 * parallel way
 * 
 * @author Amah AHITE
 * 
 */
public class StagingFlowController<P, R> {
	private final AtomicLong runningCount = new AtomicLong();
	private final long maxParallel;

	private final ConcurrentLinkedQueue<InvocationAsync<P, R>> pendings = new ConcurrentLinkedQueue<InvocationAsync<P, R>>();

	public StagingFlowController(long maxParallel) {
		this.maxParallel = maxParallel;
	}

	public void run(Callback<? super R> k, P item, FunctionAsync<P, R> iterationTask) {
		if (runningCount.incrementAndGet() <= maxParallel) {
			iterationTask.apply(new IterationCallback(k), item);
		} else {
			pendings.add(new InvocationAsync<P, R>(iterationTask, k, item));
		}
	}

	public boolean isRunning() {
		return runningCount.get() > 0;
	}

	protected class IterationCallback implements Callback<R> {
		private final Callback<? super R> parent;
		
		public IterationCallback(Callback<? super R> parent) {
			this.parent = parent;
		}

		public void completed(R result) {
			// TODO catch throwable
			long remaining = runningCount.getAndDecrement();
			if (remaining > maxParallel) {
				resume();
			}

			parent.completed(result);
		}

		public void error(Throwable e) {
			// TODO catch throwable
			long remaining = runningCount.decrementAndGet();
			if (remaining > maxParallel) {
				resume();
			}

			parent.error(e);
		}
	}

	protected void resume() {
		InvocationAsync<P, R> iter;
		while ((iter = pendings.poll()) == null) {}

		iter.getTask().apply(new IterationCallback(iter.getK()), iter.getItem());
//		do{
//			iter.getTask().apply(new IterationCallback(iter.getK()), iter.getItem());
//		}while(runningCount.get() <= maxParallel && (iter = pendings.poll()) != null);
	}

}
