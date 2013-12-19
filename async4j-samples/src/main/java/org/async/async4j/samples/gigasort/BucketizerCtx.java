package org.async.async4j.samples.gigasort;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.async.async4j.samples.gigasort.pool.ByteBufferPool;

public class BucketizerCtx {
	private final PartitionTrie partitionTrie;
	private final ByteBufferPool bufferPool;
	private final AtomicLong count = new AtomicLong();
	private final AtomicInteger bucketCount = new AtomicInteger();

	public BucketizerCtx(PartitionTrie partitionTrie, ByteBufferPool bufferPool) {
		this.partitionTrie = partitionTrie;
		this.bufferPool = bufferPool;
	}


	public ByteBufferPool getBufferPool() {
		return bufferPool;
	}


	public PartitionTrie getPartitionTrie() {
		return partitionTrie;
	}


	public AtomicLong getCount() {
		return count;
	}


	public AtomicInteger getBucketCount() {
		return bucketCount;
	}
}
