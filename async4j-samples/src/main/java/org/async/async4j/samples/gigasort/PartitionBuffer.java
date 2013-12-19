package org.async.async4j.samples.gigasort;

import java.nio.ByteBuffer;

public class PartitionBuffer{
	private final Integer partitionId;
	private final ByteBuffer buffer;

	public PartitionBuffer(Integer partitionId, ByteBuffer buffer) {
		this.partitionId = partitionId;
		this.buffer = buffer;
	}
	public Integer getPartitionId() {
		return partitionId;
	}
	public ByteBuffer getBuffer() {
		return buffer;
	}
}
