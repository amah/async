package org.async.async4j.samples.gigasort;

import static org.async4j.Async.asyncFor;
import static org.async4j.Async.asyncTry;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.async.async4j.samples.gigasort.pool.ByteBufferPool;
import org.async4j.Block;
import org.async4j.Callback;
import org.async4j.FunctionAsync;
import org.async4j.flow.StagingFlowController;
import org.async4j.streams.AggregatorAsync;
import org.async4j.streams.ConsumerAsync;
import org.async4j.streams.IteratorEnumeratorAsync;

public class BucketizingAggregator implements AggregatorAsync<ByteBuffer, Void, Void>, GigasortConstants{
	private final BucketizerCtx ctx;
	private final ConsumerAsync<Bucket> consumer;

	//	private final ByteBuffer buffers[];
	private final AtomicReferenceArray<ByteBuffer> atomicBuffers; 

	private final StagingFlowController<Void, Void> bucketizeCtrl = new StagingFlowController<>(1);
	
	public BucketizingAggregator(BucketizerCtx ctx, ConsumerAsync<Bucket> consumer) {
		this.ctx = ctx;
		this.consumer = consumer;
		this.atomicBuffers = new AtomicReferenceArray<>(ctx.getPartitionTrie().getPartitionMax());
	}

	@Override
	public void aggregate(Callback<? super Void> k, final ByteBuffer inputBuffer) {
//		inputBuffer.rewind();
//		new PartitionRec(k, inputBuffer).partition();
		bucketizeCtrl.run(k, null, new FunctionAsync<Void, Void>() {
			public void apply(Callback<? super Void> k, Void v) {
				try{
					System.out.println("bucket");
					inputBuffer.rewind();
					new PartitionRec(k, ctx, inputBuffer, atomicBuffers, consumer).partition();
				}catch(Throwable e){
					k.error(e);
				}

			}
		});
	}

	@Override
	public void getValue(Callback<? super Void> k) {
		try{
			final Bucket[] buckets = new Bucket[atomicBuffers.length()];
			for (int i = 0; i < buckets.length; i++) {
				ByteBuffer buffer = atomicBuffers.get(i);
				if(buffer != null){
					buckets[i] = new Bucket("N° "+ctx.getBucketCount().getAndIncrement(), i, buffer);
				}
			}

			System.out.println("-BEFORE END-> "+ctx.getCount().get());

			asyncTry(k, null, new FunctionAsync<Void, Void>() {
				public void apply(Callback<? super Void> k, Void p) {
					asyncFor(k, new IteratorEnumeratorAsync<>(buckets), new FunctionAsync<Bucket, Void>() {
						public void apply(Callback<? super Void> k, Bucket bucket) {
							bucket.getBuffer().flip();
							System.out.println("-END- "+bucket);
							ctx.getCount().addAndGet(bucket.getBuffer().limit());
							consumer.handle((Callback<Void>)k, bucket);
						}
					});
				}
			}).asyncFinally(new Block() {
				public void apply() {
					System.out.println("-AFTER END-> "+ctx.getCount().get());
				}
			});
		}catch(Throwable e){
			k.error(e);
		}
	}

	public static class PartitionRec implements Callback<Void>{
		private final Callback<? super Void> parent;
		private final BucketizerCtx ctx;
		private final ByteBuffer inputBuffer;
		private final AtomicReferenceArray<ByteBuffer> atomicBuffers; 
		private final ConsumerAsync<Bucket> consumer;
		
		public PartitionRec(Callback<? super Void> parent, BucketizerCtx ctx, ByteBuffer inputBuffer, AtomicReferenceArray<ByteBuffer> atomicBuffers, ConsumerAsync<Bucket> consumer) {
			this.parent = parent;
			this.ctx = ctx;
			this.inputBuffer = inputBuffer;
			this.atomicBuffers = atomicBuffers;
			this.consumer = consumer;
		}

		public void completed(Void result) {
			try{
				partition();
			}catch(Throwable e){
				parent.error(e);
			}
		}
		public void error(Throwable e) {
			parent.error(e);
		}

		public void partition() {
			if(inputBuffer.remaining() == 0){
				parent.completed(null);
			}else{
				Bucket bucket = null;
				ByteBuffer buffers[] = GigasortUtil.atomicArrayCopy(atomicBuffers, new ByteBuffer[atomicBuffers.length()]);

				while (inputBuffer.remaining() > 0) {
					int partition = ctx.getPartitionTrie().partitionOf(inputBuffer, inputBuffer.position());
					
					ByteBuffer partitionOutputBuffer = buffers[partition];
					if(partitionOutputBuffer == null){
						buffers[partition] = partitionOutputBuffer = ctx.getBufferPool().get(); 
					}
					
					GigasortUtil.copyEntry(inputBuffer, partitionOutputBuffer);
					
//					GigasortUtil.checkPartitionBuffer(partitionOutputBuffer, partitionOutputBuffer.position() - ENTRY_SIZE, partitionTrie, partition);
					if(partitionOutputBuffer.remaining() == 0){
						bucket = new Bucket("N° "+ctx.getBucketCount().getAndIncrement(),partition, partitionOutputBuffer);
						bucket.setPartitionTrie(ctx.getPartitionTrie());
						buffers[partition] = null;
						break;
					}
				}
				
				GigasortUtil.atomicArrayUpdate(atomicBuffers, buffers);
				
				if(bucket != null){
					System.out.println(bucket);
					ctx.getCount().addAndGet(bucket.getBuffer().limit());
					consumer.handle(this, bucket);
				}
				else{
					parent.completed(null);
				}
			}
		}
		
		public void flushOutputBuffers(Callback<? super Void> k){
			try{
				final Bucket[] buckets = new Bucket[atomicBuffers.length()];
				for (int i = 0; i < buckets.length; i++) {
					ByteBuffer buffer = atomicBuffers.get(i);
					if(buffer != null){
						buckets[i] = new Bucket("N° "+ctx.getBucketCount().getAndIncrement(), i, buffer);
					}
				}

				System.out.println("-BEFORE END-> "+ctx.getCount().get());

				asyncTry(k, null, new FunctionAsync<Void, Void>() {
					public void apply(Callback<? super Void> k, Void p) {
						asyncFor(k, new IteratorEnumeratorAsync<>(buckets), new FunctionAsync<Bucket, Void>() {
							public void apply(Callback<? super Void> k, Bucket bucket) {
								bucket.getBuffer().flip();
								System.out.println("-END- "+bucket);
								ctx.getCount().addAndGet(bucket.getBuffer().limit());
								consumer.handle((Callback<Void>)k, bucket);
							}
						});
					}
				}).asyncFinally(new Block() {
					public void apply() {
						System.out.println("-AFTER END-> "+ctx.getCount().get());
					}
				});
			}catch(Throwable e){
				k.error(e);
			}
			
		}
	}
}
