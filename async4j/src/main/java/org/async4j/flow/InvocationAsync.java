package org.async4j.flow;

import org.async4j.Callback;
import org.async4j.FunctionAsync;

public class InvocationAsync<P, R> {
	private final FunctionAsync<P, R> fn;
	private final Callback<? super R> k;
	private final P item;
	public InvocationAsync(FunctionAsync<P, R> task, Callback<? super R> k, P item) {
		super();
		this.fn = task;
		this.k = k;
		this.item = item;
	}
	public FunctionAsync<P, R> getTask() {
		return fn;
	}
	public Callback<? super R> getK() {
		return k;
	}
	public P getItem() {
		return item;
	}
}
