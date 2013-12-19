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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

import org.apache.commons.io.IOUtils;
import org.async.async4j.samples.gigasort.GigasortCmd.EntryIndexComparator;
import org.async.async4j.samples.gigasort.pool.ByteBufferPool;
import org.async.async4j.samples.gigasort.pool.CachingBufferPool;
import org.async4j.Block;
import org.async4j.Callback;
import org.async4j.FunctionAsync;
import org.async4j.nio.AsynchronousFileByteChannel;
import org.async4j.streams.ConsumerAsync;
import org.async4j.streams.ProducerAsync;
import org.javasimon.SimonManager;
import org.javasimon.Split;

public class PartitionProducerAsync implements ProducerAsync<ByteBuffer>, GigasortConstants{
	private final File inputFile;
	private final int bufferCount;
	private final int bufferSize;
	private final Executor pool;
	
	public PartitionProducerAsync(File inputFile, int bufferCount, int bufferSize, Executor pool) {
		super();
		this.inputFile = inputFile;
		this.bufferCount = bufferCount;
		this.bufferSize = bufferSize;
		this.pool = pool;
	}


	@Override
	public void generate(Callback<Void> k, final ConsumerAsync<ByteBuffer> handler) {
		try{
			Path path = Paths.get(inputFile.toURI());
			final AsynchronousFileChannel afc = AsynchronousFileChannel.open(path, StandardOpenOption.READ);

			asyncTry(k, null, new FunctionAsync<Void, Void>() {
				public void apply(Callback<? super Void> k, Void p) {
					AsynchronousFileByteChannel afbc = new AsynchronousFileByteChannel(afc);
					
					// Creating bufferCount+1 buffers, bufferCount to be processed in parallel and one being loaded
					final ConcurrentLinkedQueue<ByteBuffer> bufferQueue = new ConcurrentLinkedQueue<>();
					for (int i = 0; i < (bufferCount + 1); i++) {
						bufferQueue.add(ByteBuffer.allocate(bufferSize));
					}
					final ByteBufferPool bufferPool = new CachingBufferPool(bufferQueue);
					
					
					asyncParallelFor(k, new BufferLoaderAsync(afbc, bufferPool), bufferCount, withPool(pool, new FunctionAsync<ByteBuffer, Void>() {
						public void apply(final Callback<? super Void> k, ByteBuffer buffer) {
							try{
//								System.out.println("start sort of "+buffer);
								
								Split split = SimonManager.getStopwatch("partition.sort").start();

								final ByteBuffer sortedBuffer = sort(buffer);
								
								split.stop();
								
								buffer.clear();
								bufferPool.unget(buffer);
								
//								System.out.println("End sort of "+buffer);


								Callback<Void> cbk = new Callback<Void>() {
									public void completed(Void result) {
										k.completed(null);
									}

									public void error(Throwable e) {
										k.error(e);
									}
								};
								handler.handle(cbk, sortedBuffer);
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

	
	public ByteBuffer sort(ByteBuffer buffer){
		int entryCount = buffer.limit() / ENTRY_SIZE;
		Integer[] indexes = new Integer[entryCount];
		for (int i = 0; i < indexes.length; i++) {
			indexes[i] = i;
		}
		
		Arrays.sort(indexes, new EntryIndexComparator(buffer));
		
		byte[] src = buffer.array();
		byte[] dst = new byte[src.length];
		for (int i = 0; i < indexes.length; i++) {
			Integer index = indexes[i];
			System.arraycopy(src, index * 100, dst, i * 100, ENTRY_SIZE);
		}
		
		return ByteBuffer.wrap(dst);
	}
	

}
