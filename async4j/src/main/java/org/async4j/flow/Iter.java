package org.async4j.flow;

import org.async4j.Callback;
import org.async4j.Task;

public class Iter<E> {
	private final Task<E, Void> task;
	private final Callback<Void> k;
	private final E item;
	public Iter(Task<E, Void> task, Callback<Void> k, E item) {
		super();
		this.task = task;
		this.k = k;
		this.item = item;
	}
	public Task<E, Void> getTask() {
		return task;
	}
	public Callback<Void> getK() {
		return k;
	}
	public E getItem() {
		return item;
	}
}
