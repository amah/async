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
package org.async4j.streams;

import java.util.Iterator;

import org.async4j.Callback;

/**
 * Asynchronous equivalent of {@link Iterator} interface less the method {@link Iterator#remove()}. 
 * This interface is intended to be used as source of data in loop constructs (for, parallelFor etc...).  
 * @author Amah AHITE
 *
 * @param <E> The asynchronous iterator element type
 */
public interface IteratorAsync<E> {
	/**
	 * Returns asynchronously <tt>true</tt> if the iterator has more elements
	 * @param k callback listening the result
	 */
	public void hasNext(Callback<Boolean> k);
	/**
	 * Returns asynchronously the next element in the iterator
	 * @param k callback listening the result
	 */
	public void next(Callback<E> k);
}
