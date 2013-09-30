package org.async4j.exceptions;

import org.async4j.Async;
import org.async4j.Block;
import org.async4j.Callback;
import org.async4j.Function;
import org.async4j.FunctionAsync;

public class TryAsyncRunner<P, R> {
	private final Callback<? super R> k;
	private final P p;
	private final FunctionAsync<P, R> fn;
	private FunctionAsync<Throwable, R> catchFn;

	public TryAsyncRunner(Callback<? super R> k, P p, FunctionAsync<P, R> fn) {
		this.k = k;
		this.p = p;
		this.fn = fn;
	}
	
	public TryAsyncRunner<P, R> asyncCatch(FunctionAsync<Throwable, R> catchFn){
		this.catchFn = catchFn;
		return this;
	}
	
	public void _asyncCatch(FunctionAsync<Throwable, R> catchFn){
		new TryAsync<P, R>(fn, catchFn, null).apply(k, p);
	}
	
	public void asyncFinally(FunctionAsync<Void, Void> finallyFn){
		new TryAsync<P, R>(fn, catchFn, finallyFn).apply(k, p);
	}
	
	public void asyncFinally(Function<Void, Void> finallyFn){
		new TryAsync<P, R>(fn, catchFn, Async.toAsync(finallyFn)).apply(k, p);
	}
	public void asyncFinally(Block finallyFn){
		new TryAsync<P, R>(fn, catchFn, Async.toAsync(finallyFn)).apply(k, p);
	}
}
