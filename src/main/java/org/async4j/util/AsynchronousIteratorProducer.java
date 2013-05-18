package org.async4j.util;

import org.async4j.Callback;
/**
 * {@link Producer} implementation that generate element from an {@link AsynchronousIterator}.
 * @author Amah AHITE
 *
 * @param <E>
 */
public class AsynchronousIteratorProducer<E> implements Producer<E>{
	private final AsynchronousIterator<E> iterator;
	
	public AsynchronousIteratorProducer(AsynchronousIterator<E> iterator) {
		super();
		this.iterator = iterator;
	}
	
	public void produce(Callback<Void> k, ElementHandler<E> partHandler) {
		new Iterater(k, iterator, partHandler).iterate();
	}

	protected class Iterater {
		private AsynchronousIterator<E> enumerator;
		private final HasNextCallback hasNextCallback;
		private final Callback<Void> parent;
		
		
		public Iterater(Callback<Void> parent, AsynchronousIterator<E> enumerator, ElementHandler<E> partHandler) {
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
		private AsynchronousIterator<E> iterator;
		private final Callback<E> nextCallback;

		public HasNextCallback(Callback<Void> parent, AsynchronousIterator<E> iterator,  Iterater iterater,
				ElementHandler<E> aph) {
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
		private final ElementHandler<E> partHandler;
		private final IterateCallback iterateCallback;
		

		public NextCallback(Callback<Void> parent, ElementHandler<E> partHandler, Iterater iterater) {
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
