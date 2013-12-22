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

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.async4j.Callback2;

@Deprecated // use IteratorProducerAsync instead
public class RangeEnumerator implements EnumeratorAsync<Integer>{
	private volatile int start;
	private final int end;
	
	// For test only
	private Executor pool = Executors.newSingleThreadExecutor(new ThreadFactory() {
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setDaemon(true);
			return t;
		}
	});
	
	public RangeEnumerator(int end) {
		this(0, end);
	}
	
	public RangeEnumerator(int start, int end) {
		this.start = start;
		this.end = end;
	}

	public void next(final Callback2<Boolean, ? super Integer> k) {
		try{
			if(start < end){
//				k.completed(true, start++);
				if(start % 100 == 0){
					pool.execute(new Runnable() {
						public void run() {
//							System.out.println(start);
							k.completed(true, start++);
						}
					});
				}
				else{
					k.completed(true, start++);
				}
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
