package org.async.async4j.samples.gigasort.pool;

import java.nio.ByteBuffer;

// TODO to be replaced by true Object pool implementation
public interface ByteBufferPool {
	public ByteBuffer get();
	public void unget(ByteBuffer buffer);
}
