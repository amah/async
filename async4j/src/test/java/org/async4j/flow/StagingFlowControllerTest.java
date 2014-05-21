package org.async4j.flow;

import static org.async4j.Async.asyncParallelFor;
import static org.async4j.Async.range;
import static org.async4j.Async.withPool;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.async4j.Callback;
import org.async4j.FunctionAsync;
import org.async4j.FutureCallback;
import org.junit.Assert;
import org.junit.Test;

public class StagingFlowControllerTest {

	@Test
	public void stagingFlowControllerTest(){
		final Executor pool = Executors.newFixedThreadPool(10);

		final int maxCall = 2;
			FutureCallback<Void> k = new FutureCallback<Void>();

			final StagingFlowController<Void, Void> fc = new StagingFlowController<Void, Void>(maxCall);
			final AtomicInteger actualMax = new AtomicInteger();
			final AtomicInteger counter = new AtomicInteger();
			
			asyncParallelFor(k, range(0, 10000), 100, withPool(pool, new FunctionAsync<Integer, Void>(){
				public void apply(Callback<? super Void> k, final Integer item) {
					try{
						fc.run(k, null, new FunctionAsync<Void, Void>() {
							public void apply(Callback<? super Void> k, Void p) {
								int n = counter.incrementAndGet();
								if(n > actualMax.get()){
									actualMax.set(n);
								}
								counter.decrementAndGet();
								
								k.completed(null);
							}
						});
					}catch(Throwable e){
						k.error(e);
					}
					
				}
			}));
			
			k.getResult();
			
			Assert.assertEquals(maxCall, actualMax.get());
	}

}
