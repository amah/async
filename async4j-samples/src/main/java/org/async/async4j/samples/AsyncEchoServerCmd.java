package org.async.async4j.samples;

import static org.async4j.Async.*;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

import org.async4j.Callback;
import org.async4j.FutureCallback;
import org.async4j.FunctionAsync;
import org.async4j.nio.ByteChannelEnumeratorAsync;
import org.async4j.nio.ServerSocketEnumeratorAsync;
import org.kohsuke.args4j.Option;

public class AsyncEchoServerCmd implements Cmd {
	@Option(name="-count", required=true)
	private int count = 10;
	@Option(name="-poolSize", required=false)
	private int poolSize = Runtime.getRuntime().availableProcessors();
	
	@Override
	public void execute() throws Exception {
		AsynchronousServerSocketChannel assc = null;
		try{
			assc = AsynchronousServerSocketChannel.open();
			assc.bind(null, 0);
			
			FutureCallback<Void> k = new FutureCallback<>();

			asyncParallelFor(k, new ServerSocketEnumeratorAsync(assc), 2, new FunctionAsync<AsynchronousSocketChannel, Void>() {
				public void apply(Callback<? super Void> k, final AsynchronousSocketChannel asc) {
					final ByteBuffer buffer = ByteBuffer.allocate(1024);
					
					asyncFor(k, new ByteChannelEnumeratorAsync(asc, buffer), new FunctionAsync<ByteBuffer, Void>() {
						public void apply(final Callback<? super Void> k, final ByteBuffer p) {
							
							asc.write(p, null, new CompletionHandler<Integer, Void>(){
								public void completed(Integer result, Void v) {
									if(p.remaining() > 0){
										asc.write(p, null, this); // Recursion equivalent to a loop
									}
									else{k.completed(null);}
								}

								public void failed(Throwable exc, Void v) {k.error(exc);}
							});
						}
					});
				}
			});
			
			k.getResult();
		}finally{
			assc.close();
		}
	}

}
