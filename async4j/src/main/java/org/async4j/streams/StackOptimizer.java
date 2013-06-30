package org.async4j.streams;

public class StackOptimizer {
	private Thread thread;
	private Thread completionThread;

	public void recursionEnded(){
		completionThread = Thread.currentThread();
	}
	
	public boolean continueRecursion(){
		return thread != Thread.currentThread();
	}

	public void recursion(){
		thread = Thread.currentThread();
		completionThread = null;
	}
	
	public boolean continueLoop(){
		return completionThread == Thread.currentThread();
	}
	
	public void loopEnded(){
		thread = null;
	}
	
	
}
