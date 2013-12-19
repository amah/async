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

import org.async4j.exceptions.TryAsync;
import org.async4j.exceptions.TryAsyncBuilder;
import org.async4j.exceptions.TryAsyncRunner;
import org.async4j.flow.FlowControllerFactory;
import org.async4j.flow.MultiEmiterFlowControllerFactory;
import org.async4j.flow.SingleEmiterBoundFlowControllerFactory;
import org.async4j.foreach.ForEachAsync;
import org.async4j.foreach.parallel.ParallelForEachAsync;
import org.async4j.streams.AggregatingFunctionAsync;
import org.async4j.streams.AggregatorAsync;
import org.async4j.streams.EnumeratorAsync;
import org.async4j.streams.EnumeratorProducerAsync;
import org.async4j.streams.IteratorEnumeratorAsync;
import org.async4j.streams.IteratorProducerAsync;
import org.async4j.streams.ProducerAsync;
import org.async4j.streams.RangeIterable;

public class Async {
	public static <P, R> R sync(P p, FunctionAsync<P, R> task) {
		FutureCallback<R> syncK = new FutureCallback<R>();
		task.apply(syncK, p);
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
	public static <P, R> FunctionAsync<P, R> withPool(final Executor executor,
			final FunctionAsync<P, R> fn) {
		return new FunctionAsync<P, R>() {
			public void apply(final Callback<? super R> k, final P p) {
				try {
					executor.execute(new Runnable() {
						public void run() {
							try {
								fn.apply(k, p);
							} catch (Throwable e) {
								k.error(e);
							}
						}
					});
				} catch (Throwable e) {
					k.error(e);
				}

			}
		};
	}

	/**
	 * Run synchronously the specified asynchronous task, the call blocks until
	 * the end of asynchronous task.
	 * 
	 * @param task
	 *            asynchronous task to be called
	 * @return the value returned by the asynchronous task
	 */
	public static <R> R sync(FunctionAsync<Void, R> task) {
		FutureCallback<R> syncK = new FutureCallback<R>();
		task.apply(syncK, null);
		return syncK.getResult();
	}

	public static <P, R> TryAsyncBuilder<P, R> asyncTry(FunctionAsync<P, R> fn){
		return new TryAsyncBuilder<P, R>(fn);
	}
	

	public static <P, R> TryAsyncRunner<P, R> asyncTry(Callback<? super R> k, P p, FunctionAsync<P, R> fn){
		return new TryAsyncRunner<P, R>(k, p, fn);
	}
	
	public static <P, R> FunctionAsync<P, R> toAsync(final Function<P, R> fn){
		return new FunctionAsync<P, R>() {
			public void apply(Callback<? super R> k, P p) {
				try{
					k.completed(fn.apply(p));
				}catch(Throwable e){k.error(e);}
			}
		};
	}
	
	public static FunctionAsync<Void, Void> toAsync(final Block block){
		return new FunctionAsync<Void, Void>() {
			public void apply(Callback<? super Void> k, Void v) {
				try{
					block.apply();
					k.completed(null);
				}catch(Throwable e){k.error(e);}
			}
		};
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
	public static <E> void asyncFor(Callback<? super Void> k, EnumeratorAsync<E> enumerator,
			FunctionAsync<E, Void> iterationTask) {
		try {
			new ForEachAsync<E>(iterationTask).apply(k, enumerator);
		} catch (Throwable e) {
			k.error(e);
		}
	}
	public static <E> void asyncFor(Callback<? super Void> k, Iterable<E> iterable,
			FunctionAsync<E, Void> iterationTask) {
		try {
			new ForEachAsync<E>(iterationTask).apply(k, new IteratorEnumeratorAsync<E>(iterable));
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
			ProducerAsync<E> producer, FlowControllerFactory fc,
			FunctionAsync<E, Void> iterationTask) {
		try {
			ParallelForEachAsync<E> parallelForEach = new ParallelForEachAsync<E>(fc,
					iterationTask);
			parallelForEach.apply(k, producer);
		} catch (Throwable e) {
			k.error(e);
		}
	}

	public static <E, IR, R> void asyncParallelFor(final Callback<? super R> k,
			ProducerAsync<E> producer, FlowControllerFactory fc,
			final AggregatorAsync<IR, Void, R> aggregator,
			FunctionAsync<E, IR> iterationTask) {
		try {

			ParallelForEachAsync<E> parallelForEach = new ParallelForEachAsync<E>(fc,
					new AggregatingFunctionAsync<E, IR>(iterationTask, aggregator));

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

			parallelForEach.apply(aggregateValueK, producer);
		} catch (Throwable e) {
			k.error(e);
		}
	}

	public static <E, IR, R> void asyncParallelFor(final Callback<? super R> k,
			ProducerAsync<E> producer, long maxParallel,
			final AggregatorAsync<IR, Void, R> aggregator,
			FunctionAsync<E, IR> iterationTask) {
		try {
			asyncParallelFor(k, producer, new MultiEmiterFlowControllerFactory(
					maxParallel), aggregator, iterationTask);
		} catch (Throwable e) {
			k.error(e);
		}
	}

	public static <E> void asyncParallelFor(Callback<? super Void> k,
			ProducerAsync<E> producer, long maxParallel,
			FunctionAsync<E, Void> iterationTask) {
		try {
			asyncParallelFor(k, producer, new MultiEmiterFlowControllerFactory(
					maxParallel), iterationTask);
		} catch (Throwable e) {
			k.error(e);
		}
	}

	public static <E, IR, R> void asyncParallelFor(Callback<? super R> k,
			EnumeratorAsync<E> enumerator, long maxParallel,
			final AggregatorAsync<IR, Void, R> aggregator,
			FunctionAsync<E, IR> iterationTask) {
		try {
			asyncParallelFor(k, new EnumeratorProducerAsync<E>(enumerator),
					new SingleEmiterBoundFlowControllerFactory(maxParallel),
					aggregator, iterationTask);
		} catch (Throwable e) {
			k.error(e);
		}
	}

	public static <E> void asyncParallelFor(Callback<? super Void> k,
			EnumeratorAsync<E> enumerator, long maxParallel,
			FunctionAsync<E, Void> iterationTask) {
		try {
			asyncParallelFor(k, new EnumeratorProducerAsync<E>(enumerator),
					new SingleEmiterBoundFlowControllerFactory(maxParallel),
					iterationTask);
		} catch (Throwable e) {
			k.error(e);
		}
	}

	public static <E, IR, R> void asyncParallelFor(Callback<? super R> k,
			Iterable<E> iterator, long maxParallel,
			final AggregatorAsync<IR, Void, R> aggregator,
			FunctionAsync<E, IR> iterationTask) {
		try {
			asyncParallelFor(k, new IteratorProducerAsync<E>(iterator),
					new SingleEmiterBoundFlowControllerFactory(maxParallel),
					aggregator, iterationTask);
		} catch (Throwable e) {
			k.error(e);
		}
	}

	public static <E> void asyncParallelFor(Callback<? super Void> k,
			Iterable<E> enumerator, long maxParallel,
			FunctionAsync<E, Void> iterationTask) {
		try {
			asyncParallelFor(k, new IteratorProducerAsync<E>(enumerator),
					new SingleEmiterBoundFlowControllerFactory(maxParallel),
					iterationTask);
		} catch (Throwable e) {
			k.error(e);
		}
	}

	public static Iterable<Integer> range(int start, int end) {
		return new RangeIterable(start, end);
	}
}
