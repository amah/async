package org.async.async4j.samples.gigasort;

import static org.async4j.Async.asyncParallelFor;
import static org.async4j.Async.asyncTry;
import static org.async4j.Async.withPool;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.IOUtils;
import org.async.async4j.samples.gigasort.pool.ByteBufferPool;
import org.async.async4j.samples.gigasort.pool.CachingBufferPool;
import org.async4j.Block;
import org.async4j.Callback;
import org.async4j.FunctionAsync;
import org.async4j.nio.AsynchronousFileByteChannel;
import org.async4j.streams.AggregatorAsync;
import org.async4j.streams.ConsumerAsync;
import org.async4j.streams.ProducerAsync;

public class SortedBufferProducerAsync implements ProducerAsync<ByteBuffer>{
	private final List<File> sources;
	private final int splitBufferSize;
	private final int sortedOutputBufferSize;
	private final Executor pool;
	private final AtomicLong byteCount;
	
	public SortedBufferProducerAsync(AtomicLong byteCount, List<File> sources, int splitBufferSize, int sortedOutputBufferSize,
			Executor pool) {
		this.byteCount = byteCount;
		this.sources = sources;
		this.splitBufferSize = splitBufferSize;
		this.sortedOutputBufferSize = sortedOutputBufferSize;
		this.pool = pool;
	}


	@Override
	public void generate(Callback<Void> k, ConsumerAsync<ByteBuffer> handler) {
		final MergeSortAggregator msa = new MergeSortAggregator(byteCount, new AtomicInteger(sources.size()), sortedOutputBufferSize, handler);
		
		asyncParallelFor(k, sources, sources.size(), new CompletionAggregator(msa), withPool(pool, new FunctionAsync<File, Void>() {
			public void apply(Callback<? super Void> k, File file) {
				try{
					Path path = Paths.get(file.toURI());
					final AsynchronousFileChannel afc = AsynchronousFileChannel.open(path, StandardOpenOption.READ);

					asyncTry(k, null, new FunctionAsync<Void, Void>() {
						public void apply(Callback<? super Void> k, Void p) {
							AsynchronousFileByteChannel afbc = new AsynchronousFileByteChannel(afc);
							
							final ConcurrentLinkedQueue<ByteBuffer> bufferQueue = new ConcurrentLinkedQueue<>();
							for (int i = 0; i < 2; i++) {
								bufferQueue.add(ByteBuffer.allocate(splitBufferSize));
							}
							final ByteBufferPool bufferPool = new CachingBufferPool(bufferQueue);
							
							
							asyncParallelFor(k, new BufferLoaderAsync(afbc, bufferPool), 1, new MergeSourceConduit(msa), withPool(pool, new FunctionAsync<ByteBuffer, Void>() {
								public void apply(final Callback<? super Void> k, final ByteBuffer buffer) {
									buffer.flip();
									try{
										msa.aggregate(new Callback<Void>() {
											public void completed(Void result) {
												try{
//													buffer.clear();
													bufferPool.unget(buffer);
													k.completed(null);
												}catch(Throwable e){
													k.error(e);
												}
											}
											public void error(Throwable e) {k.error(e);}
										}, buffer);
									}
									catch(Throwable e){
										k.error(e);
									}
								}
							}));
						}
					}).asyncFinally(new Block() {
						public void apply() {
							IOUtils.closeQuietly(afc);
						}
					});
					
				}catch(Throwable e){
					k.error(e);
				}
			}
		}));
	}

	private class CompletionAggregator implements AggregatorAsync<Void, Void, Void>{
		private final MergeSortAggregator mergeSortAggregator;
		
		public CompletionAggregator(MergeSortAggregator mergeSortAggregator) {
			this.mergeSortAggregator = mergeSortAggregator;
		}

		@Override
		public void aggregate(Callback<? super Void> k, Void e) {
			k.completed(null);
		}

		@Override
		public void getValue(Callback<? super Void> k) {
			mergeSortAggregator.getValue(k);
		}
	}
}
