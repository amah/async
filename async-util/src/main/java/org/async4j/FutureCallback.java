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

import java.util.concurrent.Future;

/**
 * This implementation of {@link Callback} interface provides {@link Future}
 * like functionality. Invocation to method {@link #getResult()} get blocked
 * until {@link #completed} or {@link #error} are called on this callback.
 */
public class FutureCallback<R> implements Callback<R> {
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
