package org.async4j.foreach;

import static org.junit.Assert.assertEquals;
import static org.async4j.Async.*;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.async4j.Async;
import org.async4j.Callback;
import org.async4j.SynchronousCallback;
import org.async4j.Task;
import org.async4j.util.RangeEnumerator;
import org.junit.Test;

public class ParallelForEachTaskTest {
	@Test
	public void simpleForEachTest(){
		final AtomicInteger counter = new AtomicInteger();
		SynchronousCallback<Void> k = new SynchronousCallback<Void>();
		
		asyncParallelFor(k, new RangeEnumerator(0, 10), 2, new Task<Integer, Void>(){
			public void run(Callback<Void> k, Integer p) {
				
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
		SynchronousCallback<Void> k = new SynchronousCallback<Void>();
		asyncParallelFor(k, new RangeEnumerator(0, 10), 2, withPool(pool, new Task<Integer, Void>(){
			public void run(Callback<Void> k, Integer p) {
				
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
		SynchronousCallback<Void> k = new SynchronousCallback<Void>();
		asyncParallelFor(k, new RangeEnumerator(0, 10), 2, withPool(pool, new Task<Integer, Void>(){
			public void run(Callback<Void> k, Integer p) {
				
				counter.incrementAndGet();
				
				k.completed(null);
			}
		}));
		k.getResult();
		
		assertEquals(10, counter.get());
	}
}
