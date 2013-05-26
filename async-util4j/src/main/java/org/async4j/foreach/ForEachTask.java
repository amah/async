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

	public void run(Callback<? super Void> k, Enumerator<E> enumerator) {
		try {
			enumerator.next(new NextCallback<E>(k, enumerator, iterationTask));
		} catch (Throwable e) {
			k.error(e);
		}
	}

	public static class NextCallback<E> implements PairCallback<Boolean, E> {
		private final Callback<? super Void> parent;
		private final Callback<Void> iterationCallback;
		private final Task<E, Void> iterationTask;

		public NextCallback(Callback<? super Void> parent, Enumerator<E> enumerator, Task<E, Void> iterationTask) {
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
		private final Callback<? super Void> parent;
		private final PairCallback<Boolean, E> nextCallback;
		private final Enumerator<E> enumerator;

		public IterationCallback(Callback<? super Void> parent, PairCallback<Boolean, E> nextCallback, Enumerator<E> enumerator) {
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
