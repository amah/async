package org.async4j.streams;

import java.util.Iterator;

import org.async4j.Callback;
/**
 * A {@link ProducerAsync} implementation that generates elements from {@link Iterator}.
 */
public class IteratorProducerAsync<E> implements ProducerAsync<E> {
	private final Iterable<E> iterable;
	
	public IteratorProducerAsync(Iterable<E> iterable) {
		super();
		this.iterable = iterable;
	}

	public void generate(Callback<? super Void> k, ConsumerAsync<E> handler) {
		try{
			new HandlerCallback(k, iterable.iterator(), handler).completed(null);
		}catch (Throwable e) {
			k.error(e);
		}
	}

	private class HandlerCallback implements Callback<Void> {
		private final Callback<? super Void> parent;
		private final ConsumerAsync<E> consumer;
		private final Iterator<E> iterator;

		private final StackOptimizer so = new StackOptimizer();

		public HandlerCallback(Callback<? super Void> parent, Iterator<E> iterator, ConsumerAsync<E> handler) {
			this.parent = parent;
			this.iterator = iterator;
			this.consumer = handler;
		}

		public void completed(Void v) {
			try {
				so.recursionEnded();
				
				if(so.continueRecursion()){

					boolean hasNext;
					do {
						so.recursion();
						
						hasNext = iterator.hasNext();
						if(hasNext){
							consumer.handle(this, iterator.next());
						}
						else{
							parent.completed(null);
						}
					} while (hasNext && so.continueLoop());
					
					so.loopEnded();
				}
			} catch (Throwable e) {
				parent.error(e);
			}
		}

		public void error(Throwable e) {
			parent.error(e);
		}

	}
}
