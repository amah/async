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
 * Callback that is passed to the asynchronous exception handler task.
 * 
 * @author Amah AHITE
 * @param <R>
 *            the result type
 */
public class CatchCallback<R> implements Callback<R> {
	private final Callback<? super R> parent;
	private final FunctionAsync<Void, Void> finallyTask;

	public CatchCallback(Callback<? super R> parent, FunctionAsync<Void, Void> finallyTask) {
		super();
		this.parent = parent;
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
			parent.error(e);
		}
	}

	public void error(Throwable e) {
		try {
			if(finallyTask != null){
				finallyTask.apply(new FinallyCallback<R>(parent, null, e), null);
			}else{
				parent.error(e);
			}
		} catch (Throwable ex) {
			parent.error(ex);
		}
	}

}
