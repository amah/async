package org.async.async4j.samples.gigasort.pool;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CachingBufferPool implements ByteBufferPool{
	private final ConcurrentLinkedQueue<ByteBuffer> bufferQueue;

	public CachingBufferPool(ConcurrentLinkedQueue<ByteBuffer> bufferQueue) {
		this.bufferQueue = bufferQueue;
	}

	@Override
	public ByteBuffer get() {
		return bufferQueue.poll();
	}

	@Override
	public void unget(ByteBuffer buffer) {
		buffer.clear();
		bufferQueue.add(buffer);
	}

	
}
