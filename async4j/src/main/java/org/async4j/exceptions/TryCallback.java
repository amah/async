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
 * A callback to be passed to asynchronous task to manage exception handling
 * 
 * @author Amah AHITE
 * 
 * @param <R>
 */
public class TryCallback<R> implements Callback<R> {
	private final Callback<? super R> parent;
	private final FunctionAsync<Throwable, R> catchTask;
	private final FunctionAsync<Void, Void> finallyTask;

	public TryCallback(Callback<? super R> parent, FunctionAsync<Throwable, R> catchTask, FunctionAsync<Void, Void> finallyTask) {
		super();
		this.parent = parent;
		this.catchTask = catchTask;
		this.finallyTask = finallyTask;
	}

	public void completed(R result) {
		try {
			if(finallyTask != null){
				finallyTask.apply(new FinallyCallback<R>(parent, result, null), null);
			}else{
				parent.completed(result);
			}
		} catch (Throwable e) {
			error(e);
		}
	}

	public void error(Throwable e) {
		try {
			if(catchTask != null){
				catchTask.apply(new CatchCallback<R>(parent, finallyTask), e);
			}else{
				if(finallyTask != null){
					finallyTask.apply(new FinallyCallback<R>(parent, null, e), null);
				}else{
					parent.error(e);
				}
			}
		} catch (Throwable ex) {
			parent.error(ex);
		}
	}

}
