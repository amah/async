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
package org.async4j.pipe;

import org.async4j.Callback;
import org.async4j.Task;

/**
 * The pipe callback is used internally by PipeTask to chain two asynchronous tasks. It is passed to the first task and on its completion pass asynchronously the 
 * output as input to the second task. When the first Task report an error (call to {@link #error(Throwable)} the second task is not called and the error is forwarded 
 * to the parent callback.  
 * @author Amah AHITE
 *
 * @param <P> The type of the first asynchronous task result which corresponds to the type of the second task input parameter
 * @param <R> The type of the second asynchronous task result 
 */
public class PipeCallback<P, R> implements Callback<P>{
	private Callback<R> parentCallback;
	private Task<P, R> nextTask;
	
	public PipeCallback(Task<P, R> nextTask, Callback<R> parentCallback) {
		super();
		this.nextTask = nextTask;
		this.parentCallback = parentCallback;
	}

	public void completed(P p) {
		nextTask.run(parentCallback, p);
	}

	public void error(Throwable e) {
		parentCallback.error(e);
	}

}
