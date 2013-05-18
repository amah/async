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
/**
 * Interface that define asynchronous method template. Implementor of {@link #run(Callback, Object)} method have to 
 * conform to contract defined on {@link Callback} interface.
 * @author Amah AHITE
 *
 * @param <P> The asynchronous operation input parameter type
 * @param <R> The asynchronous operation output value type
 */
public interface Task<P, R> {
	/**
	 * The asynchronous method template .
	 * @param k the callback object to be called on normal or abnormal completion 
	 * @param p the parameter of the method
	 */
	public void run(Callback<R> k, P p);
}
