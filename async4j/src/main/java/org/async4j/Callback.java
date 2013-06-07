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
 * The callback interface is passed to asynchronous operations which must conform to following rules.
 * <ol>
 * 	<li>Either method {@link #completed(Object)} or {@link #error(Object)} MUST be called to report completion of the asynchronous task</li>
 * 	<li>Asynchronous operation MUST not throw exception </li>
 * </ol>
 * @author Amah AHITE
 * @param <R> The returned result type
 */
public interface Callback<R> {
	/**
	 * This method is called by asynchronous operation to notify processing end along with the result.
	 * Implementor of this method MUST not throw any exception. 
	 * @param result asynchronous operation result 
	 */
	public void completed(R result);
	
	/**
	 * Method called to report exception.
	 * Implementor of this method MUST not throw any exception. 
	 * @param e exception to be reported
	 */
	public void error(Throwable e);
}
