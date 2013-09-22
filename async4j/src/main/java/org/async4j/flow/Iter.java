package org.async4j.flow;

import org.async4j.Callback;
import org.async4j.FunctionAsync;

public class Iter<E> {
	private final FunctionAsync<E, Void> fn;
	private final Callback<Void> k;
	private final E item;
	public Iter(FunctionAsync<E, Void> task, Callback<Void> k, E item) {
		super();
		this.fn = task;
		this.k = k;
		this.item = item;
	}
	public FunctionAsync<E, Void> getTask() {
		return fn;
	}
	public Callback<Void> getK() {
		return k;
	}
	public E getItem() {
		return item;
	}
}
