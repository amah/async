package org.async.async4j.samples;

import static org.async4j.Async.asyncParallelFor;
import static org.async4j.Async.withPool;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

import org.async4j.Callback;
import org.async4j.FutureCallback;
import org.async4j.Task;
import org.async4j.streams.RangeEnumerator;
import org.kohsuke.args4j.Option;

public class ParallelForBenchCmd implements Cmd {
	@Option(name="-count", required=true)
	private int count = 10;
	@Option(name="-poolSize", required=false)
	private int poolSize = Runtime.getRuntime().availableProcessors();
	@Option(name="-fj", required=false)
	private boolean forkjoin = false;
	
	@Override
	public void execute() throws Exception {
		
		
		ExecutorService pool;
		if(forkjoin){
			pool = new ForkJoinPool(poolSize, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
		}
		else{
			pool = Executors.newFixedThreadPool(poolSize);
		}
			
		try{
			FutureCallback<Void> k = new FutureCallback<>();
			
			asyncParallelFor(k, new RangeEnumerator(count), 2, withPool(pool, new Task<Integer, Void>() {
				public void run(Callback<? super Void> k, Integer i) {

					k.completed(null);
				}
			}));
			
			k.getResult();
		}finally{
			pool.shutdown();
		}
	}

}
