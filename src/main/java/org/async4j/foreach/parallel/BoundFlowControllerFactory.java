package org.async4j.foreach.parallel;

import org.async4j.Callback;

public class BoundFlowControllerFactory implements FlowControllerFactory {
	private long maxParallel = 1;
	
	public BoundFlowControllerFactory(long maxParallel) {
		this.maxParallel = maxParallel;
	}

	public <E> FlowController<E> create(Callback<Void> iterationCallback) {
		return new BoundFlowController<E>(iterationCallback, maxParallel);
	}

}
