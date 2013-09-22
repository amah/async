package org.async4j.streams;

import java.io.ObjectInputStream.GetField;

import org.async4j.Callback;

/**
 * Interface that define asynchronous aggregation contract where elements are accumulated through the method {@link #aggregate(Callback, Object)} 
 * and resulting value provided by {@link #getValue(Callback)} method.
 * @author Amah AHITE
 * @param <E> element to be accumulated type
 * @param <I> intermediary value type
 * @param <R> the result type
 */
public interface AggregatorAsync<E,I,R> {
	/**
	 * To add asynchronously one element to the aggregator. The value reported to the callback is the intermediary value of the aggregator including the side effet of this call. 
	 * @param k the callback
	 * @param e element to be accumulated
	 */
	public void aggregate(Callback<? super I> k, E e);
	/**
	 * Returns asynchronously the current value of the aggregator
	 * @param k the callback
	 */
	public void getValue(Callback<? super R> k);

}
