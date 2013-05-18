package org.async4j.foreach.parallel;

import org.async4j.Callback;

public interface FlowControllerFactory {
	public <E> FlowController<E> create(Callback<Void> iterationCallback);
}
