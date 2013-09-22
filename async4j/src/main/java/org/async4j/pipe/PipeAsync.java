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
import org.async4j.FunctionAsync;

/**
 * Pipe task chain two tasks using {@link PipeCallback}. The second task is
 * invoked only when the first task successfully completes.
 * 
 * @author Amah AHITE
 * 
 * @param <P>
 *            Input type of the first asynchronous task
 * @param <I>
 *            Output type of the first asynchronous task and input type of the
 *            second asynchronous task
 * @param <R>
 *            Output type of the second asynchronous type
 */
public class PipeAsync<P, I, R> implements FunctionAsync<P, R> {
	private final FunctionAsync<P, I> firstTask;
	private final FunctionAsync<I, R> nextTask;

	public PipeAsync(FunctionAsync<P, I> firstTask, FunctionAsync<I, R> nextTask) {
		this.firstTask = firstTask;
		this.nextTask = nextTask;
	}

	public void apply(Callback<? super R> k, P p) {
		try {
			firstTask.apply(new PipeCallback<I, R>(k, nextTask), p);
		} catch (Throwable e) {
			k.error(e);
		}
	}
}
