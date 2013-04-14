package org.async4j;

import java.util.concurrent.Future;

/**
 * This implementation of {@link Callback} interface provides {@link Future}
 * like functionnality. Invocation to method {@link #getResult()} get blocked
 * until {@link #completed} or {@link #error} are called.
 */
public class SynchronousCallback<R> implements Callback<R> {
	private volatile R result;
	private volatile Throwable e;
	private volatile boolean completed = false;
	private final Object mutex = new Object();

	public void completed(R result) {
		this.result = result;
		unlock();
	}

	public void error(Throwable e) {
		this.e = e;
		unlock();
	}

	private void unlock() {
		synchronized (mutex) {
			if (completed) {
				throw new IllegalStateException("Already completed");
			}
			this.completed = true;
			mutex.notifyAll();
		}
	}

	/**
	 * Returns result value of the asynchronous operation if completed normally
	 * else throw a {@link RuntimeException} that wrap exception reported to
	 * this callback. If this method is called before this callback is notified,
	 * the calling thread stay blocked until asynchronous operation completion.
	 */
	public R getResult() throws RuntimeException {
		synchronized (mutex) {
			if (!completed) {
				try {
					mutex.wait();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
		if (e != null) {
			throw new RuntimeException(e);
		}
		return result;
	}
}
