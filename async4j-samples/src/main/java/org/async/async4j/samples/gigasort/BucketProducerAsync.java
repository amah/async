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
import java.util.Arrays;
import java.util.concurrent.Executor;

import org.apache.commons.io.IOUtils;
import org.async.async4j.samples.gigasort.GigasortCmd.EntryIndexComparator;
import org.async.async4j.samples.gigasort.pool.NonCachingBufferPool;
import org.async4j.Block;
import org.async4j.Callback;
import org.async4j.FunctionAsync;
import org.async4j.nio.AsynchronousFileByteChannel;
import org.async4j.streams.ConsumerAsync;
import org.async4j.streams.ProducerAsync;

public class BucketProducerAsync implements ProducerAsync<Bucket>, GigasortConstants{
	private final File inputFile;
	private final PartitionTrie partitionTrie;
	private final int bufferSize;
	private final int bucketSize;
	private final Executor pool;
	
	public BucketProducerAsync(File inputFile, PartitionTrie partitionTrie, int bufferSize, int bucketSize, Executor pool) {
		this.inputFile = inputFile;
		this.partitionTrie = partitionTrie;
		this.bufferSize = bufferSize;
		this.bucketSize = bucketSize;
		this.pool = pool;
	}

	public PartitionTrie getPartitionTrie() {
		return partitionTrie;
	}

	public int getBucketSize() {
		return bucketSize;
	}

	
	public Executor getPool() {
		return pool;
	}

	@Override
	public void generate(Callback<Void> k, final ConsumerAsync<Bucket> handler) {
		try{
			Path path = Paths.get(inputFile.toURI());
			final AsynchronousFileChannel afc = AsynchronousFileChannel.open(path, StandardOpenOption.READ);

			asyncTry(k, null, new FunctionAsync<Void, Void>() {
				public void apply(Callback<? super Void> k, Void p) {
					AsynchronousFileByteChannel afbc = new AsynchronousFileByteChannel(afc);
					
					final BucketizerCtx ctx = new BucketizerCtx(partitionTrie, new NonCachingBufferPool(bucketSize/10));
//					BucketizingAggregator bucketizingAggregator = new BucketizingAggregator(ctx, handler);
					
//					asyncParallelFor(k, new BufferLoaderAsync(afbc, new NonCachingBufferPool(bufferSize)), 10, withPool(pool, new FunctionAsync<ByteBuffer, ByteBuffer>() {
					asyncParallelFor(k, new BufferLoaderAsync(afbc, new NonCachingBufferPool(bufferSize)), 5, withPool(pool, new FunctionAsync<ByteBuffer, Void>() {
						public void apply(final Callback<? super Void> k, ByteBuffer buffer) {
							try{
								buffer.flip();
								System.out.println("Bucketize: "+buffer);
								new Bucketizer(k, ctx, buffer, handler).bucketize();
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

}
