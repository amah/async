package org.async4j.exceptions;

import org.async4j.FunctionAsync;

public class TryAsyncBuilder<P, R> {
	private final FunctionAsync<P, R> fn;
	private FunctionAsync<Throwable, R> catchFn;
	public TryAsyncBuilder(FunctionAsync<P, R> fn) {
		super();
		this.fn = fn;
	}
	
	public TryAsyncBuilder<P, R> asyncCatch(FunctionAsync<Throwable, R> catchFn){
		this.catchFn = catchFn;
		return this;
	}
	
	public FunctionAsync<P, R> _asyncCatch(FunctionAsync<Throwable, R> catchFn){
		return new TryAsync<P, R>(fn, catchFn, null);
	}
	
	public TryAsync<P, R> asyncFinally(FunctionAsync<Void, Void> finallyFn){
		return new TryAsync<P, R>(fn, catchFn, finallyFn);
	}
}
