package org.async4j.foreach.parallel;

import org.async4j.Callback;
import org.async4j.Task;

public interface FlowController<E>{
	public void run(Callback<Void> k, Task<E, Void> iterationTask, E e);
	public boolean isRunning();
}
