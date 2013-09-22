package org.async4j.streams;

import org.async4j.Callback;
import org.async4j.FunctionAsync;

public class AggregatingFunctionAsync<P,I> implements FunctionAsync<P, Void>{
	private final FunctionAsync<P, I> fn;
	private final AggregatorAsync<I, Void, ?> aggregatorAsync;
	
	
	public AggregatingFunctionAsync(FunctionAsync<P, I> task, AggregatorAsync<I, Void, ?> aggregator) {
		super();
		this.fn = task;
		this.aggregatorAsync = aggregator;
	}

	public void apply(Callback<? super Void> k, P p) {
		try{
			fn.apply(new AggregatingCallback<I>(k, aggregatorAsync), p);
		}catch(Throwable e){
			k.error(e);
		}
	}

	public static class AggregatingCallback<I> implements Callback<I>{
		private final Callback<? super Void> parent;
		private final AggregatorAsync<I, Void, ?> aggregatorAsync;
		
		public AggregatingCallback(Callback<? super Void> parent,
				AggregatorAsync<I, Void, ?> aggregator) {
			super();
			this.parent = parent;
			this.aggregatorAsync = aggregator;
		}

		public void completed(I i) {
			try{
				aggregatorAsync.aggregate(parent, i);
			}catch(Throwable e){
				parent.error(e);;
			}
		}

		public void error(Throwable e) {
			parent.error(e);
		}
	}
}
