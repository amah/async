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
