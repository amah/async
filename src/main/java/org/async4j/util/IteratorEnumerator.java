package org.async4j.util;

import java.util.Arrays;
import java.util.Iterator;

import org.async4j.PairCallback;

/**
 * {@link Enumerator} implementation that enumerate elements from a synchronous
 * {@link Iterator}
 * 
 * @author Amah AHITE
 * 
 * @param <E>
 */
public class IteratorEnumerator<E> implements Enumerator<E> {
	private final Iterator<E> iterator;

	public IteratorEnumerator(E ... e) {
		this(Arrays.asList(e));
	}

	public IteratorEnumerator(Iterable<E> iterable) {
		this.iterator = iterable.iterator();
	}

	public IteratorEnumerator(Iterator<E> iterator) {
		this.iterator = iterator;
	}

	public void next(PairCallback<Boolean, E> k) {
		try {
			boolean b = iterator.hasNext();
			E e = b ? iterator.next() : null;
			k.completed(b, e);
		} catch (Throwable e) {
			k.error(e);
		}
	}

}
