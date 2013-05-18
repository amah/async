package org.async4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.async4j.pipe.PipeTask;
import org.junit.Test;

public class PipeTaskTest {
	@Test
	public void simpleTest() {
		String s = Async.sync(10, new PipeTask<Integer, Long, String>(new Task<Integer, Long>() {
			public void run(Callback<Long> k, Integer p) {
				k.completed(10 * 2L);
			}
		}, new Task<Long, String>() {
			public void run(Callback<String> k, Long p) {
				k.completed(p.toString());
			}
		}));

		assertEquals("20", s);
	}

	@Test
	public void exceptionTest() {
		final String mesg = "Test";
		try {
			String s = Async.sync(10, new PipeTask<Integer, Long, String>(new Task<Integer, Long>() {
				public void run(Callback<Long> k, Integer p) {
					k.error(new RuntimeException(mesg));
				}
			}, new Task<Long, String>() {
				public void run(Callback<String> k, Long p) {
					k.completed(p.toString());
				}
			}));
			fail("Exception expected");
		} catch (RuntimeException e) {
			assertEquals(mesg, e.getCause().getMessage());
		}
	}
}
