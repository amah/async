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
package org.async4j.foreach.parallel;

import org.async4j.Callback;
import org.async4j.FunctionAsync;
import org.async4j.flow.FlowControllerFactory;
import org.async4j.streams.ProducerAsync;

/**
 * Parallel loop construct
 * @author Amah AHITE
 *
 * @param <E> Element 
 */
public class ParallelForEachAsync<E> implements FunctionAsync<ProducerAsync<E>, Void> {
	private final FunctionAsync<E, Void> iterationTask;
	private final FlowControllerFactory fcf;
	
	public ParallelForEachAsync(FlowControllerFactory fcf, FunctionAsync<E, Void> iterationTask) {
		this.fcf = fcf;
		this.iterationTask = iterationTask;
	}

	public void apply(Callback<? super Void> k, ProducerAsync<E> producer) {
		try{
			ParallelForEachSM<E> sm = new ParallelForEachSM<E>(k, fcf, iterationTask);
			producer.generate(sm.getProducerCallback(), sm.getElementHandler());
		}catch (Throwable e) {
			k.error(e);
		}
	}

}
