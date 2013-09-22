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
package org.async4j.exceptions;

import org.async4j.Callback;
import org.async4j.FunctionAsync;

/**
 * FunctionAsync that implements asynchronous version of the try/catch/finally blocks.
 * @author Amah AHITE
 *
 * @param <P> asynchronous input parameter type
 * @param <R> asynchronous output parameter type
 */
public class TryAsync<P, R> implements FunctionAsync<P, R> {
	private final FunctionAsync<P, R> fn;
	private final FunctionAsync<Throwable, R> catchTask;
	private final FunctionAsync<Void, Void> finallyTask;

	
	public TryAsync(FunctionAsync<P, R> task, FunctionAsync<Throwable, R> catchTask, FunctionAsync<Void, Void> finallyTask) {
		super();
		this.fn = task;
		this.catchTask = catchTask;
		this.finallyTask = finallyTask;
	}


	public void apply(Callback<? super R> k, P p) {
		try{
			fn.apply(new TryCallback<R>(k, catchTask, finallyTask), p);
		}
		catch (Throwable e) {
			k.error(e);
		}
	}
}
