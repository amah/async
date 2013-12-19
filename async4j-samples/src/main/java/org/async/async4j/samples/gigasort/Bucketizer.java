package org.async.async4j.samples.gigasort;

import static org.async4j.Async.asyncFor;
import static org.async4j.Async.asyncTry;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.async4j.Block;
import org.async4j.Callback;
import org.async4j.FunctionAsync;
import org.async4j.streams.ConsumerAsync;
import org.async4j.streams.IteratorEnumeratorAsync;


public class Bucketizer implements Callback<Void>{
	private final Callback<? super Void> parent;
	private final BucketizerCtx ctx;
	private final ByteBuffer inputBuffer;
	private final AtomicReferenceArray<ByteBuffer> atomicBuffers; 
	private final ConsumerAsync<Bucket> consumer;
	
	public Bucketizer(Callback<? super Void> parent, BucketizerCtx ctx, ByteBuffer inputBuffer, ConsumerAsync<Bucket> consumer) {
		this.parent = parent;
		this.ctx = ctx;
		this.inputBuffer = inputBuffer;
		this.atomicBuffers = new AtomicReferenceArray<>(ctx.getPartitionTrie().getPartitionMax());
		this.consumer = consumer;
	}

	public void completed(Void result) {
		try{
			bucketize();
		}catch(Throwable e){
			parent.error(e);
		}
	}
	public void error(Throwable e) {
		parent.error(e);
	}

	public void bucketize() {
		if(inputBuffer.remaining() == 0){
			// No more element in input buffer, flush output buffers
			flushOutputBuffers(parent);
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
				
//				GigasortUtil.checkPartitionBuffer(partitionOutputBuffer, partitionOutputBuffer.position() - ENTRY_SIZE, partitionTrie, partition);
				if(partitionOutputBuffer.remaining() == 0){
					bucket = new Bucket("N° "+ctx.getBucketCount().getAndIncrement(),partition, partitionOutputBuffer);
					bucket.setPartitionTrie(ctx.getPartitionTrie());
					buffers[partition] = null;
					break;
				}
			}
			
			GigasortUtil.atomicArrayUpdate(atomicBuffers, buffers);
			
			if(bucket != null){
//				System.out.println(bucket);
				ctx.getCount().addAndGet(bucket.getBuffer().limit());
				consumer.handle(this, bucket);
			}
			else{
				flushOutputBuffers(parent);
//				parent.completed(null);
			}
		}
	}
	
	public void flushOutputBuffers(Callback<? super Void> k){
		try{
			final List<Bucket> buckets = new ArrayList<>();
			int bufferCount = atomicBuffers.length();
			for (int i = 0; i < bufferCount; i++) {
				ByteBuffer buffer = atomicBuffers.get(i);
				if(buffer != null){
					buckets.add(new Bucket("N° "+ctx.getBucketCount().getAndIncrement(), i, buffer));
				}
			}

//			System.out.println("-BEFORE END-> "+ctx.getCount().get());

			asyncTry(k, null, new FunctionAsync<Void, Void>() {
				public void apply(Callback<? super Void> k, Void p) {
					asyncFor(k, new IteratorEnumeratorAsync<>(buckets), new FunctionAsync<Bucket, Void>() {
						public void apply(Callback<? super Void> k, Bucket bucket) {
							bucket.getBuffer().flip();
//							System.out.println("-END- "+bucket);
							ctx.getCount().addAndGet(bucket.getBuffer().limit());
							consumer.handle((Callback<Void>)k, bucket);
						}
					});
				}
			}).asyncFinally(new Block() {
				public void apply() {
//					System.out.println("-AFTER END-> "+ctx.getCount().get());
				}
			});
		}catch(Throwable e){
			k.error(e);
		}
		
	}
}
