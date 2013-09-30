package org.async.async4j.samples;

import static org.async4j.Async.asyncFor;
import static org.async4j.Async.asyncParallelFor;
import static org.async4j.Async.asyncTry;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import org.apache.commons.io.IOUtils;
import org.async4j.Block;
import org.async4j.Callback;
import org.async4j.FunctionAsync;
import org.async4j.FutureCallback;
import org.async4j.nio.ByteChannelEnumeratorAsync;
import org.async4j.nio.ServerSocketEnumeratorAsync;
import org.kohsuke.args4j.Option;

public class AsyncEchoServerCmd implements Cmd {
	@Option(name="-maxcon", required=false)
	private int maxcon = 100;
	@Option(name="-address", required=false)
	private String address = "localhost";
	@Option(name="-port", required=false)
	private int port = 4321;
	
	@Override
	public void execute() throws Exception {
		AsynchronousServerSocketChannel assc = null;
		try{
			assc = AsynchronousServerSocketChannel.open();
			assc.bind(new InetSocketAddress(address,port), 0);
			
			FutureCallback<Void> k = new FutureCallback<>();

			asyncParallelFor(k, new ServerSocketEnumeratorAsync(assc), maxcon, new FunctionAsync<AsynchronousSocketChannel, Void>() {
				public void apply(Callback<? super Void> k, final AsynchronousSocketChannel asc) {
					
					asyncTry(k, null, new FunctionAsync<Void,Void>(){
						public void apply(Callback<? super Void> k, Void v){
							final ByteBuffer buffer = ByteBuffer.allocate(1024);
							
							logConnection(asc);
							
							asyncFor(k, new ByteChannelEnumeratorAsync(asc, buffer), new FunctionAsync<ByteBuffer, Void>() {
								public void apply(final Callback<? super Void> k, final ByteBuffer p) {
									
									buffer.flip();

									asc.write(p, null, new CompletionHandler<Integer, Void>(){
										public void completed(Integer result, Void v) {
											if(p.remaining() > 0){
												asc.write(p, null, this); // Recursion equivalent to a loop
											}
											else{
												buffer.compact();
												k.completed(null);
											}
										}
		
										public void failed(Throwable exc, Void v) {k.error(exc);}
									});
								}
							});
							
						}
						
					}).asyncFinally(new Block() {
						public void apply() {
							System.out.println("Closing connection");
							IOUtils.closeQuietly(asc);
						}
					});
				}
			});
			
			k.getResult();
		}finally{
			assc.close();
		}
	}

	public static void logConnection(AsynchronousSocketChannel asc){
		try {
			System.out.println("connected from "+asc.getRemoteAddress());
		} catch (IOException e) {
			throw new RuntimeException("failed to get remote address", e);
		}
	}
}