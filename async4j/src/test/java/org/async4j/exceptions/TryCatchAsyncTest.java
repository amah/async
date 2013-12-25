package org.async4j.exceptions;

import static org.async4j.Async.asyncTry;

import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.Assert;

import org.async4j.Callback;
import org.async4j.FunctionAsync;
import org.async4j.FutureCallback;
import org.junit.Test;

public class TryCatchAsyncTest {
	
	@Test
	public void tryCatchFinallyTest(){
		final String msg = "Test exception mesg";
		final AtomicBoolean catchFlag = new AtomicBoolean();
		final AtomicBoolean finallyFlag = new AtomicBoolean();
		try{
			FutureCallback<Void> k = new FutureCallback<Void>();

			asyncTry(k, null, new FunctionAsync<Void, Void>() {
				public void apply(Callback<? super Void> k, Void p) {
					try{
						throw new RuntimeException(msg);
					}catch(Throwable e) {
						k.error(e);
					}
				}
			}).asyncCatch(new FunctionAsync<Throwable, Void>() {
				public void apply(Callback<? super Void> k, Throwable p) {
					catchFlag.set(p.getMessage().equals(msg));
					k.completed(null);
				}
			}).asyncFinally(new FunctionAsync<Void, Void>() {
				public void apply(Callback<? super Void> k, Void p) {
					finallyFlag.set(true);
					k.completed(null);
				}
			});
			
			k.getResult();
		}catch(Exception e){
			Assert.assertTrue(catchFlag.get());
			Assert.assertTrue(finallyFlag.get());
		}
	}

	@Test(expected=RuntimeException.class)
	public void tryCatchRethrowFinallyTest(){
		final String msg = "Test exception mesg";
		final AtomicBoolean catchFlag = new AtomicBoolean();
		final AtomicBoolean finallyFlag = new AtomicBoolean();
		try{
			FutureCallback<Void> k = new FutureCallback<Void>();

			asyncTry(k, null, new FunctionAsync<Void, Void>() {
				public void apply(Callback<? super Void> k, Void p) {
					try{
						throw new RuntimeException(msg);
					}catch(Throwable e) {
						k.error(e);
					}
				}
			}).asyncCatch(new FunctionAsync<Throwable, Void>() {
				public void apply(Callback<? super Void> k, Throwable p) {
					catchFlag.set(p.getMessage().equals(msg));
					k.error(p);
				}
			}).asyncFinally(new FunctionAsync<Void, Void>() {
				public void apply(Callback<? super Void> k, Void p) {
					finallyFlag.set(true);
					k.completed(null);
				}
			});
			
			k.getResult();
			
		}finally{
			Assert.assertTrue(catchFlag.get());
			Assert.assertTrue(finallyFlag.get());
		}
	}
}
