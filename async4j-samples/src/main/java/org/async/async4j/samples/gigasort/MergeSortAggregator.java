package org.async.async4j.samples.gigasort;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousByteChannel;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.async4j.Callback;
import org.async4j.streams.AggregatorAsync;
import org.async4j.streams.ConsumerAsync;
import org.javasimon.SimonManager;
import org.javasimon.Split;

public class MergeSortAggregator implements AggregatorAsync<ByteBuffer, Void, Void>{
	private final ConcurrentLinkedQueue<MergeSortItem> buffers = new ConcurrentLinkedQueue<>();
	private final AtomicInteger sourceCount;
//	private final AtomicLong byteCount;
	private volatile ByteBuffer outputBuffer;
	private final ConsumerAsync<ByteBuffer> outputConsumer;
	private final int bufferSize;
	

	public MergeSortAggregator(AtomicLong byteCount, AtomicInteger sourceCount, int bufferSize, ConsumerAsync<ByteBuffer> outputConsumer) {
		super();
//		this.byteCount = byteCount;
		this.sourceCount = sourceCount;
		this.bufferSize = bufferSize;
		this.outputConsumer = outputConsumer;
	}

	@Override
	public void aggregate(Callback<? super Void> k, ByteBuffer buffer) {
		buffer.rewind();
		buffers.add(new MergeSortItem(buffer, k));
		if(buffers.size() == sourceCount.get()){
			merge();
		}
	}

	@Override
	public void getValue(Callback<? super Void> k) {
		try{
//			System.out.println("Global getValue: "+byteCount.get());
			if(outputBuffer != null){
				outputBuffer.flip();
				outputConsumer.handle((Callback<Void>)k, outputBuffer);
			}else{
				k.completed(null);
			}
		}catch(Throwable e){
			k.error(e);
		}
	}

	public void decrementSource(){
		sourceCount.decrementAndGet();
		merge();
	}

	private void merge() {
		Split split = SimonManager.getStopwatch("mergesort.sort").start();

		if(buffers.size() == 0){
			return;
		}
		if(outputBuffer == null){
			outputBuffer = ByteBuffer.allocate(bufferSize);
		}
		MergeSortItem firstEmpty = null;
		if(buffers.size() == 1){
			MergeSortItem item = buffers.peek();
//			System.out.printf("Merge - sourceCnt = %s, in=%s, out=%s\n",buffers.size(), item.buffer, outputBuffer);
			int n = GigasortUtil.copy(item.buffer, outputBuffer);
//			byteCount.addAndGet(n);
			if(item.buffer.remaining() == 0){
				firstEmpty = item;
			}
		}
		else{
			MergeSortItem[] items = new MergeSortItem[buffers.size()];
			Iterator<MergeSortItem> iter = buffers.iterator();
			for (int i = 0; i < items.length; i++) {
				items[i] = iter.next();
			}

			while (firstEmpty == null && outputBuffer.remaining() != 0) {

				MergeSortItem min = items[0];
				for (int i = 1; i < items.length; i++) {
					MergeSortItem msi = items[i];
					if(GigasortUtil.compare(msi.buffer, msi.position, min.buffer, min.position) < 0){
						min = msi; 
					}
				}
				
				min.transferEntry(outputBuffer);
				if(min.buffer.remaining() == 0){
					firstEmpty = min; 
				}
			}
		}
		
		split.stop();
		if(outputBuffer.remaining() == 0){
			ByteBuffer b= outputBuffer;
			outputBuffer = null;
			Callback<? super Void> emptiedBufferCallback = null;
			if(firstEmpty != null){
				emptiedBufferCallback = firstEmpty.callback;
				buffers.remove(firstEmpty);
			}
			b.flip();
			outputConsumer.handle(new ConsumerCallback(emptiedBufferCallback), b);
		}
		else{
			if(firstEmpty != null){
//				System.out.printf("Merge - sourceCnt = %s, %s\n",buffers.size(), outputBuffer);
				buffers.remove(firstEmpty);
				firstEmpty.callback.completed(null);
			}
		}
	}

	public class MergeSortItem implements Comparable<MergeSortItem>{
		public final ByteBuffer buffer;
		public final Callback<? super Void> callback;
		public volatile int position;
		public MergeSortItem(ByteBuffer buffer, Callback<? super Void> callback) {
			super();
			this.buffer = buffer;
			this.callback = callback;
		}
		public void transferEntry(ByteBuffer dst) {
//			byteCount.addAndGet(GigasortConstants.ENTRY_SIZE);
			GigasortUtil.copyEntry(buffer, dst);
			position += 1;
		}
		@Override
		public int compareTo(MergeSortItem o) {
			return GigasortUtil.compare(buffer, position, o.buffer, o.position);
		}
	}
	
	public class ConsumerCallback implements Callback<Void>{
		private final Callback<? super Void> emptiedBufferCallback;
		
		public ConsumerCallback(Callback<? super Void> emptiedBufferCallback) {
			this.emptiedBufferCallback = emptiedBufferCallback;
		}

		public void completed(Void result) {
			if(emptiedBufferCallback != null){
				emptiedBufferCallback.completed(null);
			}
			else{
				merge();
			}
		}

		public void error(Throwable e) {
			for (MergeSortItem item : buffers) {
				item.callback.error(e);
			}
			if(emptiedBufferCallback != null){
				emptiedBufferCallback.error(e);
			}
		}
	}
}
