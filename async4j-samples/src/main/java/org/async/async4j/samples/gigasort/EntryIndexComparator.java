package org.async.async4j.samples.gigasort;

import java.nio.ByteBuffer;
import java.util.Comparator;

public class EntryIndexComparator implements Comparator<Integer>, GigasortConstants{
	private final ByteBuffer buffer;
	
	public EntryIndexComparator(ByteBuffer buffer) {
		this.buffer = buffer;
	}

	@Override
	public int compare(Integer o1, Integer o2) {
		int offset1 = o1 * 100;
		int offset2 = o2 * 100;
		for(int i = 0; i < KEY_SIZE; i++){
			int value1 = buffer.get(offset1++) & 0xFF;
			int value2 = buffer.get(offset2++) & 0xFF;
			if(value1 < value2){
				return -1;
			}
			else if (value1 > value2){
				return 1;
			}
		}
		
		return 0;
	}
}