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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.async4j.exceptions.Try;
import org.async4j.pipe.PipeTask;
import org.junit.Test;

public class TryTaskTest {

	@Test
	public void noExceptionTest() {
		final String mesg = "Test ---";
		final AtomicLong n = new AtomicLong(0);
		final AtomicBoolean catchExecuted = new AtomicBoolean();
		final AtomicBoolean finallyExecuted = new AtomicBoolean();
		try {
			String s = Async.sync(new Try<Void, String>(new PipeTask<Void, Long, String>(new Task<Void, Long>() {
				public void run(Callback<? super Long> k, Void p) {
					k.completed(n.incrementAndGet());
				}
			}, new Task<Long, String>() {
				public void run(Callback<? super String> k, Long p) {
					k.completed(""+n.incrementAndGet());
				}
			}), new Task<Throwable, String>() {
				public void run(Callback<? super String> k, Throwable p) {
					catchExecuted.set(true);
					k.completed(p.getMessage());
				}
			}, new Task<Void, Void>() {
				public void run(Callback<? super Void> k, Void p) {
					finallyExecuted.set(true);
					k.completed(null);
				}
			}));
			assertEquals("2", s);
		} catch (RuntimeException e) {
			fail("No Exception expected");
		}
		assertEquals(n.get(), 2L);
		assertTrue(!catchExecuted.get());
		assertTrue(finallyExecuted.get());
	}
	@Test
	public void exceptionTest() {
		final String mesg = "Test ---";
		final AtomicLong n = new AtomicLong(0);
		final AtomicBoolean catchExecuted = new AtomicBoolean();
		final AtomicBoolean finallyExecuted = new AtomicBoolean();
		try {
			String s = Async.sync(new Try<Void, String>(new PipeTask<Void, Long, String>(new Task<Void, Long>() {
				public void run(Callback<? super Long> k, Void p) {
					k.completed(n.incrementAndGet());
				}
			}, new Task<Long, String>() {
				public void run(Callback<? super String> k, Long p) {
					k.error(new RuntimeException(mesg));
				}
			}), new Task<Throwable, String>() {
				public void run(Callback<? super String> k, Throwable p) {
					catchExecuted.set(true);
					k.completed(p.getMessage());
				}
			}, new Task<Void, Void>() {
				public void run(Callback<? super Void> k, Void p) {
					finallyExecuted.set(true);
					k.completed(null);
				}
			}));
			assertEquals(mesg, s);
		} catch (RuntimeException e) {
			fail("No Exception expected");
		}
		assertTrue(n.get() == 1);
		assertTrue(catchExecuted.get());
		assertTrue(finallyExecuted.get());
	}
}
