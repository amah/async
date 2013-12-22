package org.async.async4j.samples.gigasort;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousByteChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.sql.PooledConnection;

import org.async.async4j.samples.gigasort.pool.ByteBufferPool;
import org.async4j.Callback;
import org.async4j.streams.ConsumerAsync;
import org.async4j.streams.ProducerAsync;

public class BufferLoaderAsync implements ProducerAsync<ByteBuffer>{
	private final AsynchronousByteChannel abc;
	private final ByteBufferPool bufferPool;
	
	public BufferLoaderAsync(AsynchronousByteChannel abc, ByteBufferPool bufferPool) {
		this.abc = abc;
		this.bufferPool = bufferPool;
	}

	@Override
	public void generate(Callback<? super Void> k, ConsumerAsync<ByteBuffer> handler) {
		try{
			ByteBuffer buffer = bufferPool.get();
			abc.read(buffer, null,  new LoaderCompletionHandler(buffer, k, handler));
		}catch(Throwable e){k.error(e);}
	}

	public class LoaderCompletionHandler implements CompletionHandler<Integer, Void>{
		private final ByteBuffer buffer;
		private final Callback<? super Void> parent;
		private final ConsumerAsync<ByteBuffer> handler;
		
		
		public LoaderCompletionHandler(ByteBuffer buffer, Callback<? super Void> parent, ConsumerAsync<ByteBuffer> handler) {
			this.buffer = buffer;
			this.parent = parent;
			this.handler = handler;
		}

		@Override
		public void completed(Integer result, Void attachment) {
			try{
				if(result == -1){
					if(buffer.position() == 0){
						// End of channel and nothing read
						parent.completed(null);
					}
					else{
						// End of channel but buffer not empty
						HandlerCallback handlerCallback = new HandlerCallback(parent, handler, true);
						handler.handle(handlerCallback, buffer);
					}
				}
				else if(buffer.remaining() != 0){
					abc.read(buffer, null, this);
				}
				else{
					HandlerCallback handlerCallback = new HandlerCallback(parent, handler, false);
					handler.handle(handlerCallback, buffer);
				}
			}catch(Throwable e){
				parent.error(e);
			}
		}
		
		@Override
		public void failed(Throwable exc, Void attachment) {
			parent.error(exc);
		}
	}
	
	public class HandlerCallback implements Callback<Void>{
		private final Callback<? super Void> parent;
		private final ConsumerAsync<ByteBuffer> handler;
		private final boolean ended;
		

		public HandlerCallback(Callback<? super Void> parent, ConsumerAsync<ByteBuffer> handler, boolean ended) {
			super();
			this.parent = parent;
			this.handler = handler;
			this.ended = ended;
		}

		@Override
		public void completed(Void result) {
			if(ended){
				parent.completed(null);
			}
			else{
				ByteBuffer buffer = bufferPool.get();
				abc.read(buffer, null, new LoaderCompletionHandler(buffer, parent, handler));
			}
		}

		@Override
		public void error(Throwable e) {
			parent.error(e);
		}
		
	}
}
