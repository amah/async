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
package org.async4j.streams;

import org.async4j.Callback;
/**
 * {@link ProducerAsync} implementation that generate element from an {@link IteratorAsync}.
 * @author Amah AHITE
 *
 * @param <E>
 */
public class IteratorProducerAsync<E> implements ProducerAsync<E>{
	private final IteratorAsync<E> iterator;
	
	public IteratorProducerAsync(IteratorAsync<E> iterator) {
		super();
		this.iterator = iterator;
	}
	
	public void produce(Callback<Void> k, ConsumerAsync<E> partHandler) {
		new Iterater(k, iterator, partHandler).iterate();
	}

	protected class Iterater {
		private IteratorAsync<E> enumerator;
		private final HasNextCallback hasNextCallback;
		private final Callback<Void> parent;
		
		
		public Iterater(Callback<Void> parent, IteratorAsync<E> enumerator, ConsumerAsync<E> partHandler) {
			this.enumerator = enumerator;
			this.parent = parent;
			hasNextCallback = new HasNextCallback(parent, enumerator, this, partHandler);
		}

		public void iterate() {
			try{
				enumerator.hasNext(hasNextCallback);
			}catch (Throwable e) {
				parent.error(e);
			}
		}
	}

	protected class HasNextCallback implements
			Callback<Boolean> {
		private final Callback<Void> parent;
		private IteratorAsync<E> iterator;
		private final Callback<E> nextCallback;

		public HasNextCallback(Callback<Void> parent, IteratorAsync<E> iterator,  Iterater iterater,
				ConsumerAsync<E> aph) {
			super();
			this.parent = parent;
			this.iterator = iterator;
			this.nextCallback = new NextCallback(parent, aph, iterater);
		}

		public void completed(Boolean b) {
			try{
				if (! b) {
					parent.completed(null);
					return;
				}
				else{
					iterator.next(nextCallback);
				}
			}
			catch (Throwable e) {
				parent.error(e);
			}
		}

		public void error(Throwable exc) {
			parent.error(exc);
		}

	}

	protected class NextCallback implements Callback<E> {
		private final Callback<Void> parent;
		private final ConsumerAsync<E> partHandler;
		private final IterateCallback iterateCallback;
		

		public NextCallback(Callback<Void> parent, ConsumerAsync<E> partHandler, Iterater iterater) {
			super();
			this.parent = parent;
			this.partHandler = partHandler;
			this.iterateCallback = new IterateCallback(parent, iterater);
		}

		public void completed(E result) {
			try{
				partHandler.handleElement(iterateCallback, result);
			}
			catch (Throwable e) {
				error(e);
			}
		}

		public void error(Throwable e) {
			parent.error(e);
		}
	}



	protected class IterateCallback implements Callback<Void> {
		private final Callback<Void> parent;
		private final Iterater iterater;

		public IterateCallback(Callback<Void> parent, Iterater iterater) {
			super();
			this.parent = parent;
			this.iterater = iterater;
		}

		public void completed(Void result) {
			try{
				iterater.iterate();
			}
			catch (Throwable e) {
				error(e);
			}
		}

		public void error(Throwable e) {
			parent.error(e);
		}
	}

}
