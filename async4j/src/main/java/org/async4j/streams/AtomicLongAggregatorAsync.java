package org.async4j.streams;

import java.util.concurrent.atomic.AtomicLong;

import org.async4j.Callback;

public class AtomicLongAggregatorAsync implements AggregatorAsync<Long, Void, Long> {
	private final AtomicLong value = new AtomicLong();

	public void aggregate(Callback<? super Void> k, Long delta) {
		try {
			this.value.addAndGet(delta);
			k.completed(null);
		} catch (Throwable e) {
			k.error(e);
		}
	}

	public void getValue(Callback<? super Long> k) {
		try {
			k.completed(value.get());
		} catch (Throwable e) {
			k.error(e);
		}
	}

}
