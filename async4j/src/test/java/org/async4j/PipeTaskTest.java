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
import static org.junit.Assert.fail;

import org.async4j.pipe.PipeAsync;
import org.junit.Test;

public class PipeTaskTest {
	@Test
	public void simpleTest() {
		String s = Async.sync(10, new PipeAsync<Integer, Long, String>(new FunctionAsync<Integer, Long>() {
			public void apply(Callback<? super Long> k, Integer p) {
				k.completed(10 * 2L);
			}
		}, new FunctionAsync<Long, String>() {
			public void apply(Callback<? super String> k, Long p) {
				k.completed(p.toString());
			}
		}));

		assertEquals("20", s);
	}

	@Test
	public void exceptionTest() {
		final String mesg = "Test";
		try {
			String s = Async.sync(10, new PipeAsync<Integer, Long, String>(new FunctionAsync<Integer, Long>() {
				public void apply(Callback<? super Long> k, Integer p) {
					k.error(new RuntimeException(mesg));
				}
			}, new FunctionAsync<Long, String>() {
				public void apply(Callback<? super String> k, Long p) {
					k.completed(p.toString());
				}
			}));
			fail("Exception expected");
		} catch (RuntimeException e) {
			assertEquals(mesg, e.getCause().getMessage());
		}
	}
}
