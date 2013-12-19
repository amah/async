package org.async.async4j.samples.gigasort;

import java.math.BigDecimal;
import java.nio.ByteBuffer;

public class PartitionTrie implements GigasortConstants{
	private final ByteBuffer trieBuffer;
	private final int partitionMax;
	
	public PartitionTrie(ByteBuffer trieBuffer, int partitionMax) {
		this.trieBuffer = trieBuffer;
		this.partitionMax = partitionMax;
	}


	public int partitionOf(ByteBuffer data, int offset){
		int partition = 0;
		while(partition < partitionMax - 1 && GigasortUtil.compareOffset(data, offset, trieBuffer, partition * ENTRY_SIZE) > 0){
			partition++;
		}
		
//		System.out.printf("input: %s, partition: %s \n", GigasortUtil.entryToBigDecimal(data, offset), partition);
		return partition;
	}


	public ByteBuffer getTrieBuffer() {
		return trieBuffer;
	}


	public int getPartitionMax() {
		return partitionMax;
	}
	
	
		
}
