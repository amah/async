package org.async4j.streams;

import java.util.Iterator;

import org.async4j.Callback;
/**
 * A {@link Producer} implementation that generates elements from {@link Iterator}.
 */
public class IteratorProducer<E> implements Producer<E> {
	private final Iterable<E> iterable;
	
	public IteratorProducer(Iterable<E> iterable) {
		super();
		this.iterable = iterable;
	}

	public void generate(Callback<Void> k, Handler<E> handler) {
		try{
			new HandlerCallback(k, iterable.iterator(), handler).completed(null);
		}catch (Throwable e) {
			k.error(e);
		}
	}

	private class HandlerCallback implements Callback<Void> {
		private final Callback<Void> parent;
		private final Handler<E> handler;
		private final Iterator<E> iterator;

		private final StackOptimizer so = new StackOptimizer();

		public HandlerCallback(Callback<Void> parent, Iterator<E> iterator, Handler<E> handler) {
			this.parent = parent;
			this.iterator = iterator;
			this.handler = handler;
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
							handler.handle(this, iterator.next());
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
