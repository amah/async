package org.async4j.streams;

import org.async4j.Callback;
import org.async4j.Task;

public class AggregatingTask<P,I> implements Task<P, Void>{
	private final Task<P, I> task;
	private final Aggregator<I, Void, ?> aggregator;
	
	
	public AggregatingTask(Task<P, I> task, Aggregator<I, Void, ?> aggregator) {
		super();
		this.task = task;
		this.aggregator = aggregator;
	}

	public void run(Callback<? super Void> k, P p) {
		try{
			task.run(new AggregatingCallback<I>(k, aggregator), p);
		}catch(Throwable e){
			k.error(e);
		}
	}

	public static class AggregatingCallback<I> implements Callback<I>{
		private final Callback<? super Void> parent;
		private final Aggregator<I, Void, ?> aggregator;
		
		public AggregatingCallback(Callback<? super Void> parent,
				Aggregator<I, Void, ?> aggregator) {
			super();
			this.parent = parent;
			this.aggregator = aggregator;
		}

		public void completed(I i) {
			try{
				aggregator.aggregate(parent, i);
			}catch(Throwable e){
				parent.error(e);;
			}
		}

		public void error(Throwable e) {
			parent.error(e);
		}
	}
}
