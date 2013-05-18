package org.async4j.foreach;

import org.async4j.Callback;
import org.async4j.PairCallback;
import org.async4j.Task;
import org.async4j.util.AsynchronousIterator;
import org.async4j.util.Enumerator;

/**
 * 
 * @author Amah AHITE
 * 
 * @param <E>
 *            loop iterator element type
 */
public class ForEachTask<E> implements Task<Enumerator<E>, Void> {
	private final Task<E, Void> iterationTask;

	public ForEachTask(Task<E, Void> iterationTask) {
		this.iterationTask = iterationTask;
	}

	public void run(Callback<Void> k, Enumerator<E> enumerator) {
		try {
			enumerator.next(new NextCallback<E>(k, enumerator, iterationTask));
		} catch (Throwable e) {
			k.error(e);
		}
	}

	public static class NextCallback<E> implements PairCallback<Boolean, E> {
		private final Callback<Void> parent;
		private final Callback<Void> iterationCallback;
		private final Task<E, Void> iterationTask;

		public NextCallback(Callback<Void> parent, Enumerator<E> enumerator, Task<E, Void> iterationTask) {
			this.parent = parent;
			this.iterationTask = iterationTask;
			this.iterationCallback = new IterationCallback<E>(parent, this, enumerator);
		}

		public void completed(Boolean found, E e) {
			try {
				if (found) {
					iterationTask.run(iterationCallback, e);
				} else {
					parent.completed(null);
				}
			} catch (Throwable ex) {
				parent.error(ex);
			}
		}

		public void error(Throwable e) {
			parent.error(e);
		}

	}

	public static class IterationCallback<E> implements Callback<Void> {
		private final Callback<Void> parent;
		private final PairCallback<Boolean, E> nextCallback;
		private final Enumerator<E> enumerator;

		public IterationCallback(Callback<Void> parent, PairCallback<Boolean, E> nextCallback, Enumerator<E> enumerator) {
			this.parent = parent;
			this.nextCallback = nextCallback;
			this.enumerator = enumerator;
		}

		public void completed(Void result) {
			try {
				enumerator.next(nextCallback);
			} catch (Throwable e) {
				parent.error(e);
			}
		}

		public void error(Throwable e) {
			parent.error(e);
		}
	}

}
