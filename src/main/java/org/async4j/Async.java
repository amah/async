package org.async4j;

import java.util.concurrent.Executor;

import org.async4j.foreach.ForEachTask;
import org.async4j.foreach.parallel.BoundFlowControllerFactory;
import org.async4j.foreach.parallel.ParallelForEach;
import org.async4j.util.Enumerator;
import org.async4j.util.EnumeratorProducer;
import org.async4j.util.Producer;

public class Async {
	public static <P, R> R sync(P p, Task<P, R> task) {
		SynchronousCallback<R> syncK = new SynchronousCallback<R>();
		task.run(syncK, p);
		return syncK.getResult();
	}

	public static <P, R> Task<P, R> withPool(final Executor pool, final Task<P, R> t) {
		return new Task<P, R>() {
			public void run(final Callback<R> k, final P p) {
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
		SynchronousCallback<R> syncK = new SynchronousCallback<R>();
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

	public static <E> void asyncParallelFor(Callback<Void> k, Producer<E> producer, long maxParallel,
			Task<E, Void> iterationTask) {
		try {
			ParallelForEach<E> parallelForEach = new ParallelForEach<E>(new BoundFlowControllerFactory(maxParallel),
					iterationTask);
			parallelForEach.run(k, producer);
		} catch (Throwable e) {
			k.error(e);
		}
	}
	public static <E> void asyncParallelFor(Callback<Void> k, Enumerator<E> enumerator, long maxParallel,
			Task<E, Void> iterationTask) {
		try {
			ParallelForEach<E> parallelForEach = new ParallelForEach<E>(new BoundFlowControllerFactory(maxParallel),
					iterationTask);
			parallelForEach.run(k, new EnumeratorProducer<E>(enumerator));
		} catch (Throwable e) {
			k.error(e);
		}
	}
}
