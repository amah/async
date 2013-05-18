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
