package org.async4j.util;

import org.async4j.PairCallback;

public class RangeEnumerator implements Enumerator<Integer>{
	private volatile int start;
	private final int end;
	
	public RangeEnumerator(int end) {
		this(0, end);
	}
	
	public RangeEnumerator(int start, int end) {
		this.start = start;
		this.end = end;
	}

	public void next(PairCallback<Boolean, Integer> k) {
		try{
			if(start < end){
				k.completed(true, start++);
			}
			else{
				k.completed(false, null);
			}
		}
		catch (Throwable e) {
			k.error(e);
		}
	}
}
