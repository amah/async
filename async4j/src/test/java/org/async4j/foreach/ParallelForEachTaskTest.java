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
package org.async4j.foreach;

import static org.junit.Assert.assertEquals;
import static org.async4j.Async.*;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.async4j.Async;
import org.async4j.Callback;
import org.async4j.FutureCallback;
import org.async4j.Task;
import org.async4j.streams.RangeEnumerator;
import org.junit.Test;

public class ParallelForEachTaskTest {
	@Test
	public void simpleForEachTest(){
		final AtomicInteger counter = new AtomicInteger();
		FutureCallback<Void> k = new FutureCallback<Void>();
		
		asyncParallelFor(k, new RangeEnumerator(0, 10), 2, new Task<Integer, Void>(){
			public void run(Callback<? super Void> k, Integer p) {
				
				counter.incrementAndGet();
				
				k.completed(null);
			}
		});
		k.getResult();
		
		assertEquals(10, counter.get());
	}
	@Test
	public void threadedForEachTest(){
		final AtomicInteger counter = new AtomicInteger();
		final Executor pool = Executors.newSingleThreadExecutor();
		FutureCallback<Void> k = new FutureCallback<Void>();
		asyncParallelFor(k, new RangeEnumerator(0, 10), 2, withPool(pool, new Task<Integer, Void>(){
			public void run(Callback<? super Void> k, Integer p) {
				
				counter.incrementAndGet();
				
				k.completed(null);
			}
		}));
		k.getResult();
		
		assertEquals(10, counter.get());
	}
	@Test
	public void multiThreadedForEachTest(){
		final AtomicInteger counter = new AtomicInteger();
		final Executor pool = Executors.newFixedThreadPool(5);
		FutureCallback<Void> k = new FutureCallback<Void>();
		asyncParallelFor(k, new RangeEnumerator(0, 10), 2, withPool(pool, new Task<Integer, Void>(){
			public void run(Callback<? super Void> k, Integer p) {
				
				counter.incrementAndGet();
				
				k.completed(null);
			}
		}));
		k.getResult();
		
		assertEquals(10, counter.get());
	}
}
