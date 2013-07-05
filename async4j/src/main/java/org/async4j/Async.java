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
package org.async4j;

import java.util.concurrent.Executor;

import org.async4j.flow.FlowControllerFactory;
import org.async4j.flow.MultiEmiterFlowControllerFactory;
import org.async4j.flow.SingleEmiterBoundFlowControllerFactory;
import org.async4j.foreach.ForEachTask;
import org.async4j.foreach.parallel.ParallelForEach;
import org.async4j.streams.AggregatingTask;
import org.async4j.streams.Aggregator;
import org.async4j.streams.Enumerator;
import org.async4j.streams.EnumeratorProducer;
import org.async4j.streams.IteratorProducer;
import org.async4j.streams.Producer;
import org.async4j.streams.RangeIterable;

public class Async {
	public static <P, R> R sync(P p, Task<P, R> task) {
		FutureCallback<R> syncK = new FutureCallback<R>();
		task.run(syncK, p);
		return syncK.getResult();
	}

	/**
	 * Wraps an asynchronous task with an executor.
	 * 
	 * @param executor
	 *            use to run the wrapped task
	 * @param task
	 *            the task to be wrapped
	 * @return a wrapped task
	 */
	public static <P, R> Task<P, R> withPool(final Executor executor,
			final Task<P, R> task) {
		return new ExecutorTask<P, R>(executor, task);
	}

	/**
	 * Run synchronously the specified asynchronous task, the call blocks until
	 * the end of asynchronous task.
	 * 
	 * @param task
	 *            asynchronous task to be called
	 * @return the value returned by the asynchronous task
	 */
	public static <R> R sync(Task<Void, R> task) {
		FutureCallback<R> syncK = new FutureCallback<R>();
		task.run(syncK, null);
		return syncK.getResult();
	}

	/**
	 * Asynchronous for loop
	 * 
	 * @param k
	 *            the callback notified on the completion of the loop
	 * @param enumerator
	 *            element source for the loop
	 * @param iterationTask
	 *            task to be called for each element from the enumerator
	 */
	public static <E> void asyncFor(Callback<Void> k, Enumerator<E> enumerator,
			Task<E, Void> iterationTask) {
		try {
			new ForEachTask<E>(iterationTask).run(k, enumerator);
		} catch (Throwable e) {
			k.error(e);
		}
	}

	/**
	 * Asynchronous parallel for loop.
	 * 
	 * @param k
	 *            the callback notified on the completion of the loop
	 * @param producer
	 *            generator of elements to be injected in the parallel
	 *            iterations
	 * @param maxParallel
	 *            The maximum number of parallel iteration
	 * @param iterationTask
	 *            task to be called for each element
	 */
	public static <E> void asyncParallelFor(Callback<? super Void> k,
			Producer<E> producer, FlowControllerFactory fc, Task<E, Void> iterationTask) {
		try {
			ParallelForEach<E> parallelForEach = new ParallelForEach<E>(
					fc,
					iterationTask);
			parallelForEach.run(k, producer);
		} catch (Throwable e) {
			k.error(e);
		}
	}

	public static <E, IR, R> void asyncParallelFor(final Callback<? super R> k,
			Producer<E> producer, FlowControllerFactory fc,final Aggregator<IR, Void, R> aggregator, 
			Task<E, IR> iterationTask) {
		try {
			
			ParallelForEach<E> parallelForEach = new ParallelForEach<E>(fc,
					new AggregatingTask<E, IR>(iterationTask, aggregator));
			
			Callback<Void> aggregateValueK = new Callback<Void>() {
				public void completed(Void result) {
					try {
						aggregator.getValue(k);
					} catch (Throwable e) {
						k.error(e);
					}
				}

				public void error(Throwable e) {
					k.error(e);
				}
			};
			
			parallelForEach.run(aggregateValueK, producer);
		} catch (Throwable e) {
			k.error(e);
		}
	}

	public static <E, IR, R> void asyncParallelFor(final Callback<? super R> k,
			Producer<E> producer, long maxParallel,
			final Aggregator<IR, Void, R> aggregator, Task<E, IR> iterationTask) {
		try {
			asyncParallelFor(k, producer, new MultiEmiterFlowControllerFactory(maxParallel), aggregator, iterationTask);
		} catch (Throwable e) {
			k.error(e);
		}
	}

	public static <E> void asyncParallelFor(Callback<? super Void> k,
			Producer<E> producer, long maxParallel, Task<E, Void> iterationTask) {
		try {
			asyncParallelFor(k, producer, new MultiEmiterFlowControllerFactory(maxParallel), iterationTask);
		} catch (Throwable e) {
			k.error(e);
		}
	}
	
	public static <E, IR, R> void asyncParallelFor(Callback<? super R> k,
			Enumerator<E> enumerator, long maxParallel,
			final Aggregator<IR, Void, R> aggregator, Task<E, IR> iterationTask) {
		try {
			asyncParallelFor(k, new EnumeratorProducer<E>(enumerator), new SingleEmiterBoundFlowControllerFactory(maxParallel), aggregator, iterationTask);
		} catch (Throwable e) {
			k.error(e);
		}
	}

	
	public static <E> void asyncParallelFor(Callback<? super Void> k,
			Enumerator<E> enumerator, long maxParallel,
			Task<E, Void> iterationTask) {
		try {
			asyncParallelFor(k, new EnumeratorProducer<E>(enumerator), new SingleEmiterBoundFlowControllerFactory(maxParallel), iterationTask);
		} catch (Throwable e) {
			k.error(e);
		}
	}

	
	public static <E, IR, R> void asyncParallelFor(Callback<? super R> k,
			Iterable<E> iterator, long maxParallel,
			final Aggregator<IR, Void, R> aggregator, Task<E, IR> iterationTask) {
		try {
			asyncParallelFor(k, new IteratorProducer<E>(iterator), new SingleEmiterBoundFlowControllerFactory(maxParallel), aggregator, iterationTask);
		} catch (Throwable e) {
			k.error(e);
		}
	}

	
	public static <E> void asyncParallelFor(Callback<? super Void> k,
			Iterable<E> enumerator, long maxParallel,
			Task<E, Void> iterationTask) {
		try {
			asyncParallelFor(k, new IteratorProducer<E>(enumerator), new SingleEmiterBoundFlowControllerFactory(maxParallel), iterationTask);
		} catch (Throwable e) {
			k.error(e);
		}
	}

	public static Iterable<Integer> range(int start, int end) {
		return new RangeIterable(start, end);
	}
}
