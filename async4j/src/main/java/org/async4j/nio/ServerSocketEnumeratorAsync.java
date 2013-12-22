package org.async4j.nio;

import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import org.async4j.Callback2;
import org.async4j.streams.EnumeratorAsync;

public class ServerSocketEnumeratorAsync implements
		EnumeratorAsync<AsynchronousSocketChannel> {
	private final AsynchronousServerSocketChannel serverSocketChannel;
	private final ConnectionCompletionHandler ch = new ConnectionCompletionHandler();
	
	public ServerSocketEnumeratorAsync(
			AsynchronousServerSocketChannel serverSocketChannel) {
		super();
		this.serverSocketChannel = serverSocketChannel;
	}

	public void next(Callback2<Boolean, ? super AsynchronousSocketChannel> k) {
		try {
			serverSocketChannel.accept(k, ch);
		} catch (Throwable e) {
			k.error(e);
		}
	}

	protected class ConnectionCompletionHandler implements
			CompletionHandler<AsynchronousSocketChannel, Callback2<Boolean, ? super AsynchronousSocketChannel>> {

		public void completed(AsynchronousSocketChannel result, Callback2<Boolean, ? super AsynchronousSocketChannel> k) {
			try {
				k.completed(result != null, result);
			} catch (Throwable e) {
				k.error(e);
			}
		}

		public void failed(Throwable exc, Callback2<Boolean, ? super AsynchronousSocketChannel> k) {
			if(exc instanceof AsynchronousCloseException){
				k.completed(false, null);
				return;
			}
			else{
				k.error(exc);
			}
		}

	}
}
