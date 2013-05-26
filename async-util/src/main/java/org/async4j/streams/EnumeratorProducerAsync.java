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
import org.async4j.Callback2;

/**
 * {@link ProducerAsync} implementation that generate element from an {@link EnumeratorAsync}.
 * @author Amah AHITE
 *
 * @param <E> the enumerated element type
 */
public class EnumeratorProducerAsync<E> implements ProducerAsync<E> {
	private final EnumeratorAsync<E> enumerator;

	public EnumeratorProducerAsync(EnumeratorAsync<E> enumerator) {
		this.enumerator = enumerator;
	}

	public void produce(Callback<Void> k, ConsumerAsync<E> handler) {
		try{
			enumerator.next(new NextCallback(k, handler, enumerator));
		}catch (Throwable e) {
			k.error(e);
		}
	}

	private class NextCallback implements Callback2<Boolean, E> {
		private final Callback<Void> parent;
		private final HandlerCallback handleK;
		private final ConsumerAsync<E> handler;

		public NextCallback(Callback<Void> parent, ConsumerAsync<E> handler, EnumeratorAsync<E> enumerator) {
			this.parent = parent;
			this.handler = handler;
			this.handleK = new HandlerCallback(parent, this, enumerator);
		}

		public void completed(Boolean b, E e) {
			try {
				if(b){
					handler.handleElement(handleK, e);
				}
				else{
					parent.completed(null);
				}
			} catch (Throwable t) {
				parent.error(t);
			}
		}

		public void error(Throwable e) {
			parent.error(e);
		}

	}

	private class HandlerCallback implements Callback<Void> {
		private final Callback<Void> parent;
		private final NextCallback nextK;
		private final EnumeratorAsync<E> enumerator;

		public HandlerCallback(Callback<Void> parent, NextCallback nextK, EnumeratorAsync<E> enumerator) {
			this.parent = parent;
			this.nextK = nextK;
			this.enumerator = enumerator;
		}

		public void completed(Void v) {
			try{
				enumerator.next(nextK);
			}catch (Throwable e) {
				parent.error(e);
			}
		}

		public void error(Throwable e) {
			parent.error(e);
		}

	}

}
