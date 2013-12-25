package org.async4j.foreach;

import static org.async4j.Async.asyncFor;
import static org.async4j.Async.asyncParallelFor;
import static org.async4j.Async.withPool;

import java.util.List;
import java.util.concurrent.Executor;

import org.async4j.Callback;
import org.async4j.FunctionAsync;
import org.async4j.streams.ConsumerAsync;
import org.async4j.streams.ProducerAsync;
import org.async4j.streams.RangeIterable;

public class ConcurrentRangeProducerAsync implements ProducerAsync<Integer>{
	private final Executor pool;
	private final List<Iterable<Integer>> ranges;
	public ConcurrentRangeProducerAsync(Executor pool, List<Iterable<Integer>> ranges) {
		super();
		this.pool = pool;
		this.ranges = ranges;
	}

	public void generate(Callback<? super Void> k, final ConsumerAsync<Integer> consumer) {
		asyncParallelFor(k, ranges, ranges.size(), withPool(pool, new FunctionAsync<Iterable<Integer>, Void>() {
			public void apply(Callback<? super Void> k, Iterable<Integer> p) {

				asyncFor(k, p, withPool(pool, new FunctionAsync<Integer, Void>() {
					public void apply(Callback<? super Void> k, Integer p) {
						consumer.handle((Callback<Void>)k, p);
					}
				}));
			}
		}));
	}
	
	
	
}
