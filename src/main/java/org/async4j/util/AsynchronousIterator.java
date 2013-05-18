package org.async4j.util;

import java.util.Iterator;

import org.async4j.Callback;

/**
 * Asynchronous equivalent of {@link Iterator} interface less the method {@link Iterator#remove()}. 
 * This interface is intended to be used as source of data in loop constructs (for, parallelFor etc...).  
 * @author Amah AHITE
 *
 * @param <E> The asynchronous iterator element type
 */
public interface AsynchronousIterator<E> {
	/**
	 * Returns asynchronously <tt>true</tt> if the iterator has more elements
	 * @param k callback listening the result
	 */
	public void hasNext(Callback<Boolean> k);
	/**
	 * Returns asynchronously the next element in the iterator
	 * @param k callback listening the result
	 */
	public void next(Callback<E> k);
}
