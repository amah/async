package org.async4j.util;

import org.async4j.PairCallback;

/**
 * This interface is the compacted form of {@link AsynchronousIterator} where the next element and its presence test boolean 
 * are returned by the same call.
 * @author Amah AHITE
 */
public interface Enumerator<E> {
	public void next(PairCallback<Boolean, E> k);
}
