package org.async.async4j.samples.gigasort.pool;

import java.nio.ByteBuffer;

public class NonCachingBufferPool implements ByteBufferPool{
	private final int bufferSize;

	public NonCachingBufferPool(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	@Override
	public ByteBuffer get() {
		return ByteBuffer.allocate(bufferSize);
	}

	@Override
	public void unget(ByteBuffer buffer) {
	}

	
}
