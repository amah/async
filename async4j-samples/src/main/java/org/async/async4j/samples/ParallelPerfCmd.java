package org.async.async4j.samples;

import static org.async4j.Async.*;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

import org.async4j.Callback;
import org.async4j.FutureCallback;
import org.async4j.FunctionAsync;
import org.kohsuke.args4j.Option;

public class ParallelPerfCmd implements Cmd {
	@Option(name="-count", required=false)
	private int count = 100000000;
	@Option(name="-poolSize", required=false)
	private int poolSize = Runtime.getRuntime().availableProcessors();
	@Option(name="-fj", required=false)
	private boolean forkjoin = false;
	
	@Override
	public void execute() throws Exception {
		Executor pool;
		if(poolSize == 0){
			pool = new Executor() {
				@Override
				public void execute(Runnable arg0) {
					arg0.run();
				}
			};
		}else {
			if(forkjoin){
				pool = new ForkJoinPool(poolSize, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
			}
			else{
				pool = Executors.newFixedThreadPool(poolSize);
			}
		}
			
		try{
			FutureCallback<Void> k = new FutureCallback<>();
			
			asyncParallelFor(k, range(0, count), 2, withPool(pool, new FunctionAsync<Integer, Void>() {
				public void apply(Callback<? super Void> k, Integer i) {

					k.completed(null);
				}
			}));
			
			k.getResult();
		}finally{
			if (pool instanceof ExecutorService) {
				ExecutorService executorService = (ExecutorService) pool;
				executorService.shutdown();
			}
		}
	}

}
