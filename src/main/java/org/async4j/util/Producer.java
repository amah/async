package org.async4j.util;

import org.async4j.Callback;

/**
 * 
 * @author Amah
 *
 * @param <E>
 */
public interface Producer<E> {
	public void produce(Callback<Void> k, ElementHandler<E> handler);
}
