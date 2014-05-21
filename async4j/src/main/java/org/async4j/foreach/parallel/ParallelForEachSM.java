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

import java.util.concurrent.atomic.AtomicBoolean;

import org.async4j.Callback;
import org.async4j.FunctionAsync;
import org.async4j.flow.FlowController;
import org.async4j.flow.FlowControllerFactory;
import org.async4j.streams.ConsumerAsync;


/**
 * Parallel for loop state machine that observes the producer and dispatcher to determine
 * the end of the asynchronous for loop.
 * @author Amah AHITE
 *
 * @param <E>
 */
public class ParallelForEachSM<E>{
	private final FlowController<E> flowController;
	private final Callback<? super Void> parentK;
	private final ExceptionAggregator exceptionAggregator = new DefaultExceptionAggregator();
	private final AtomicBoolean ended = new AtomicBoolean();
	private volatile boolean error = false;
	private volatile boolean producerEnded = false;
	private final FunctionAsync<E, Void> iterationTask;
	private final Callback<Void> producerCallback = new ProducerCallback();
	private final Callback<Void> iterationCallback = new IterationEndCallback();
	private final ConsumerAsync<E> elementHandler = new ProducerElementHandler();

	public ParallelForEachSM(Callback<? super Void> parentK, FlowControllerFactory fcf, FunctionAsync<E, Void> iterationTask) {
		this.parentK = parentK;
		this.flowController = fcf.create(iterationCallback);
		this.iterationTask = iterationTask;
	}

	protected void mayCompleted(){
		if(producerEnded){
			if(! flowController.isRunning()){
				if(! ended.getAndSet(true)){
					if(! error){
						parentK.completed(null);
					}
					else{
						parentK.error(exceptionAggregator.getAggregated());
					}
				}
			}
		}
	}
	
	
	public Callback<Void> getProducerCallback() {
		return producerCallback;
	}

	public ConsumerAsync<E> getElementHandler() {
		return elementHandler;
	}


	protected class ProducerElementHandler implements ConsumerAsync<E>{
		
		public void handle(Callback<Void> k, E e) {
			try{
				if(error){
					k.error(exceptionAggregator.getAggregated());
				}
				else{
					flowController.run(k, iterationTask, e);
				}
			}catch (Throwable t) {
				k.error(t);
			}
		}
	}
	protected class ProducerCallback implements Callback<Void>{
		public void completed(Void result) {
			producerEnded = true;
			mayCompleted();
		}

		public void error(Throwable e) {
			exceptionAggregator.handle(e);
			error = true;
			producerEnded = true;
			mayCompleted();
		}
	}
	
	
	protected class IterationEndCallback implements Callback<Void>{
		public void completed(Void v) {
				mayCompleted();
		}

		public void error(Throwable e) {
			exceptionAggregator.handle(e);
			error = true;
			mayCompleted();
		}
	}
}
