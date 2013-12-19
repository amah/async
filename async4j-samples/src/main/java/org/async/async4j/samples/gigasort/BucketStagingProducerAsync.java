package org.async.async4j.samples.gigasort;

import static org.async4j.Async.asyncParallelFor;

import org.async4j.Callback;
import org.async4j.FunctionAsync;
import org.async4j.streams.ConsumerAsync;
import org.async4j.streams.ProducerAsync;

public class BucketStagingProducerAsync implements ProducerAsync<Bucket>{
	private final BucketProducerAsync bucketProducerAsync;
	
	
	public BucketStagingProducerAsync(BucketProducerAsync bucketProducerAsync) {
		super();
		this.bucketProducerAsync = bucketProducerAsync;
	}


	@Override
	public void generate(Callback<Void> k, ConsumerAsync<Bucket> consumer) {
		try{
			BucketAggregator bucketAggr = new BucketAggregator(bucketProducerAsync.getPartitionTrie(), bucketProducerAsync.getBucketSize(), bucketProducerAsync.getPool(), consumer);
			asyncParallelFor(k, bucketProducerAsync, 5, bucketAggr, new FunctionAsync<Bucket, Bucket>() {
				@Override
				public void apply(Callback<? super Bucket> k, Bucket p) {
					k.completed(p);
				}
			});
		}catch(Throwable e){k.error(e);}
	}
}
