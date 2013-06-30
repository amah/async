/*******************************************************************************
 * Copyright 2013 Async4j Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.async4j.streams;

import java.util.Iterator;

public class RangeIterable implements Iterable<Integer>{
	private final int start;
	private final int end;
	
	public RangeIterable(int end) {
		this(0, end);
	}
	
	public RangeIterable(int start, int end) {
		this.start = start;
		this.end = end;
	}

	private class RangeIterator implements Iterator<Integer>{
		private volatile int start;
		public RangeIterator(int start) {
			this.start = start;
		}
		public boolean hasNext() {
			return start < end;
		}
		public Integer next() {
			return start++;
		}
		public void remove() {
			// TODO Auto-generated method stub
			
		}
	}
	
	public Iterator<Integer> iterator() {
		return new RangeIterator(start);
	}
}
