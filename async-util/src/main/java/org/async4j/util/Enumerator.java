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
package org.async4j.util;

import org.async4j.PairCallback;

/**
 * This interface is the compacted form of {@link AsynchronousIterator} where the next element and its presence test boolean 
 * are returned by the same call.
 * @author Amah AHITE
 */
public interface Enumerator<E> {
	public void next(PairCallback<Boolean, E> k);
}
