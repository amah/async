package org.async4j.foreach.parallel;

import org.async4j.Callback;
import org.async4j.Task;
import org.async4j.util.Producer;

/**
 * Parallel for construct
 * @author Amah AHITE
 *
 * @param <E> Element 
 */
public class ParallelForEach<E> implements Task<Producer<E>, Void> {
	private final Task<E, Void> iterationTask;
	private final FlowControllerFactory fcf;
	
	public ParallelForEach(FlowControllerFactory fcf, Task<E, Void> iterationTask) {
		this.fcf = fcf;
		this.iterationTask = iterationTask;
	}

	public void run(Callback<Void> k, Producer<E> producer) {
		try{
			ParallelForEachSM<E> sm = new ParallelForEachSM<E>(k, fcf, iterationTask);
			producer.produce(sm.getProducerCallback(), sm.getElementHandler());
		}catch (Throwable e) {
			k.error(e);
		}
	}

}
