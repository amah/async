package org.async.async4j.samples.gigasort;

import java.nio.ByteBuffer;

import org.async4j.Callback;
import org.async4j.streams.AggregatorAsync;

public class MergeSourceConduit implements AggregatorAsync<Void, Void, Void>{
	private final MergeSortAggregator aggregator;
	
	public MergeSourceConduit(MergeSortAggregator aggregator) {
		this.aggregator = aggregator;
	}

	@Override
	public void aggregate(Callback<? super Void> k, Void v) {
		k.completed(null);
	}

	@Override
	public void getValue(Callback<? super Void> k) {
		try{
			aggregator.decrementSource();
			k.completed(null);
		}catch(Throwable e){
			k.error(e);
		}
	}

}
