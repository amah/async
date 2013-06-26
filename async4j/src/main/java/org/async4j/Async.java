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

import org.async4j.flow.MultiEmiterFlowControllerFactory;
import org.async4j.foreach.ForEachTask;
import org.async4j.foreach.parallel.ParallelForEach;
import org.async4j.streams.Enumerator;
import org.async4j.streams.EnumeratorProducer;
import org.async4j.streams.Generator;

public class Async {
	public static <P, R> R sync(P p, Task<P, R> task) {
		FutureCallback<R> syncK = new FutureCallback<R>();
		task.run(syncK, p);
		return syncK.getResult();
	}

	public static <P, R> Task<P, R> withPool(final Executor pool, final Task<P, R> t) {
		return new Task<P, R>() {
			public void run(final Callback<? super R> k, final P p) {
				try {
					pool.execute(new Runnable() {
						public void run() {
							try {
								t.run(k, p);
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

	public static <R> R sync(Task<Void, R> task) {
		FutureCallback<R> syncK = new FutureCallback<R>();
		task.run(syncK, null);
		return syncK.getResult();
	}

	public static <E> void asyncFor(Callback<Void> k, Enumerator<E> enumerator, Task<E, Void> iterationTask) {
		try {
			new ForEachTask<E>(iterationTask).run(k, enumerator);
		} catch (Throwable e) {
			k.error(e);
		}
	}

	public static <E> void asyncParallelFor(Callback<Void> k, Generator<E> producer, long maxParallel,
			Task<E, Void> iterationTask) {
		try {
			ParallelForEach<E> parallelForEach = new ParallelForEach<E>(new MultiEmiterFlowControllerFactory(maxParallel),
					iterationTask);
			parallelForEach.run(k, producer);
		} catch (Throwable e) {
			k.error(e);
		}
	}
	public static <E> void asyncParallelFor(Callback<? super Void> k, Enumerator<E> enumerator, long maxParallel,
			Task<E, Void> iterationTask) {
		try {
			ParallelForEach<E> parallelForEach = new ParallelForEach<E>(new MultiEmiterFlowControllerFactory(maxParallel),
					iterationTask);
			parallelForEach.run(k, new EnumeratorProducer<E>(enumerator));
		} catch (Throwable e) {
			k.error(e);
		}
	}
}
