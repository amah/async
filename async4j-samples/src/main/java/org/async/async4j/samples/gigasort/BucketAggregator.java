package org.async.async4j.samples.gigasort;

import static org.async4j.Async.asyncFor;
import static org.async4j.Async.withPool;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

import org.async4j.Callback;
import org.async4j.FunctionAsync;
import org.async4j.flow.StagingFlowController;
import org.async4j.streams.AggregatorAsync;
import org.async4j.streams.ConsumerAsync;


public class BucketAggregator implements AggregatorAsync<Bucket, Void, Void>{
	private final int bucketMaxSize;
	private final ConsumerAsync<Bucket> consumer;
	private final Map<Integer, BucketStage> bucketStageMap = new HashMap<Integer, BucketStage>();
	private final PartitionTrie partitionTrie;
	private final Executor pool;

	
	public BucketAggregator(PartitionTrie partitionTrie, int bucketMaxSize, Executor pool, ConsumerAsync<Bucket> consumer) {
		super();
		this.bucketMaxSize = bucketMaxSize;
		this.consumer = consumer;
		this.partitionTrie = partitionTrie;
		this.pool = pool;
		
		for (int i = 0; i < partitionTrie.getPartitionMax(); i++) {
			bucketStageMap.put(i, new BucketStage());
		}
		
	}


	@Override
	public void aggregate(Callback<? super Void> k, Bucket bucket) {
		try{
			System.out.println("Aggrgating : "+bucket);
			bucketStageMap.get(bucket.getPartitionId()).add(k, bucket);
		}catch(Throwable e){k.error(e);}
	}


	@Override
	public void getValue(Callback<? super Void> k) {
		asyncFor(k, bucketStageMap.values(), new FunctionAsync<BucketStage, Void>() {
			public void apply(Callback<? super Void> k, BucketStage bucketStage) {
				try{
					bucketStage.flush(k);
				}catch(Throwable e){k.error(e);}
			}
		});
	}

	
	private class BucketStage{
		private volatile ConcurrentLinkedQueue<Bucket> buckets = new ConcurrentLinkedQueue<>();
		private final StagingFlowController<Void, Void> flowCtrl = new  StagingFlowController<>(1);
		
		public void add(Callback<? super Void> k, final Bucket bucket){
			flowCtrl.run(k, null, withPool(pool, new FunctionAsync<Void, Void>() {
				public void apply(Callback<? super Void> k, Void p) {
					try{
						buckets.add(bucket);
						
						int totalLen = 0;
						for (Bucket b : buckets) {
							totalLen += b.getBuffer().limit();
						}
						
						
						if(totalLen >= bucketMaxSize){
							flush(k);
						}
						else{
							k.completed(null);
						}
						
					}catch(Throwable e){k.error(e);}
				}
			}));
		}
		

		private void flush(Callback<? super Void> k) {
			try{
				if(buckets.size() > 0){
						
					Iterable<Bucket> bucketToWrite = buckets;
					
					//reset buffer
					buckets = new ConcurrentLinkedQueue<>();
	
					int outputLen = 0;
					for (Bucket b : bucketToWrite) {
						outputLen += b.getBuffer().limit();
					}
	
					ByteBuffer outputBuffer = ByteBuffer.allocate(outputLen);
					int partitionId = -1;
					for (Bucket b : bucketToWrite) {
						partitionId = b.getPartitionId();

						b.getBuffer().rewind();
						GigasortUtil.transfer(b.getBuffer(), outputBuffer);
					}
					
//					outputBuffer.flip();
					System.out.println(outputBuffer);
					Bucket outputBucket = new Bucket(partitionId, outputBuffer);
					outputBucket.setPartitionTrie(partitionTrie);
					
					System.err.println("Aggregated bucket: "+outputBucket);
					consumer.handle((Callback<Void>)k, outputBucket);
				}
				else{
					k.completed(null);
				}
			}catch(Throwable e){k.error(e);}
		}
	}
}
