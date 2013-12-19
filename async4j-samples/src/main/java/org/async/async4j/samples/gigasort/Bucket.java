package org.async.async4j.samples.gigasort;

import java.nio.ByteBuffer;

public class Bucket {
	private volatile String name;
	private final Integer partitionId;
	private final ByteBuffer buffer;
	private PartitionTrie partitionTrie;

	public Bucket(Integer partitionId, ByteBuffer buffer) {
		this.partitionId = partitionId;
		this.buffer = buffer;
	}

	public Bucket(String name, Integer partitionId, ByteBuffer buffer) {
		this.name = name;
		this.partitionId = partitionId;
		this.buffer = buffer;
	}
	public Integer getPartitionId() {
		return partitionId;
	}
	public ByteBuffer getBuffer() {
		return buffer;
	}
	
	public String getName() {
		return name;
	}

	public void checkBucket(){
//		int offset = 0;
//		while(offset < buffer.limit()){
//			if(partitionTrie.partitionOf(buffer, offset) != partitionId){
//				System.out.printf("Entry %s not in right partition: %s\n", GigasortUtil.entryToHex(buffer, offset), partitionId);
//			}
//			offset+=GigasortConstants.ENTRY_SIZE;
//		}
	}

	
	public PartitionTrie getPartitionTrie() {
		return partitionTrie;
	}

	public void setPartitionTrie(PartitionTrie partitionTrie) {
		this.partitionTrie = partitionTrie;
	}

	@Override
	public String toString() {
		return "Bucket "+(name == null ? "" : name)+" "+GigasortUtil.entryToHex(buffer, 0)+" [partitionId=" + partitionId + ", buffer=" + buffer + "]";
	}
	
}
