package org.async4j;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Assert;
import org.junit.Test;

public class SynchronousCallbackTest {
	@Test
	public void simpleTest(){
		SynchronousCallback<Integer> syncK = new SynchronousCallback<Integer>();
		new Task<Void, Integer>() {
			public void run(Callback<Integer> k, Void p) {
				k.completed(1);
			}
		}.run(syncK, null);
		
		Assert.assertEquals(1, syncK.getResult().intValue());
	}
	
	@Test
	public void threadedTest(){
		final ExecutorService pool = Executors.newSingleThreadExecutor();

		try {
			SynchronousCallback<Integer> syncK = new SynchronousCallback<Integer>();
			new Task<Void, Integer>() {
				public void run(final Callback<Integer> k, Void p) {
					pool.submit(new Runnable() {
						public void run() {
							k.completed(1);
						}
					});
				}
			}.run(syncK, null);
			
			Assert.assertEquals(1, syncK.getResult().intValue());
		} finally {
			pool.shutdown();
		}
		
		assertTrue(pool.isShutdown());
	}

	@Test
	public void exceptionTest(){
		final String mesg = "Test";
		SynchronousCallback<Integer> syncK = new SynchronousCallback<Integer>();
		new Task<Void, Integer>() {
			public void run(Callback<Integer> k, Void p) {
				k.error(new RuntimeException(mesg));
			}
		}.run(syncK, null);
		
		try {
			syncK.getResult();
			fail("Exception expected");
		} catch (RuntimeException e) {
			Assert.assertEquals(mesg, e.getCause().getMessage());
		}
	}
}
