package org.async4j.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousByteChannel;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.Future;

public class AsynchronousFileByteChannel implements AsynchronousByteChannel{
	private AsynchronousFileChannel fileChannel;
	private long readPosition = 0;
	private long writePosition = 0;
	
	public AsynchronousFileByteChannel(AsynchronousFileChannel fileChannel) {
		super();
		this.fileChannel = fileChannel;
	}
	
	public AsynchronousFileByteChannel(AsynchronousFileChannel fileChannel, long writePosition) {
		super();
		this.fileChannel = fileChannel;
		this.writePosition = writePosition;
	}
	
	private class ReadCompletionHandler<A> implements CompletionHandler<Integer, A> {
		CompletionHandler<Integer, ? super A> delegate;
		public ReadCompletionHandler(CompletionHandler<Integer, ? super A> ch) {
			super();
			this.delegate = ch;
		}

		public void completed(Integer result, A attachment) {
			readPosition += result;
			delegate.completed(result, attachment);
		}

		public void failed(Throwable exc, A attachment) {
			delegate.failed(exc, attachment);
		}
		
		
	}; 
	private class WriteCompletionHandler<A> implements CompletionHandler<Integer, A> {
		CompletionHandler<Integer, ? super A> ch;
		public WriteCompletionHandler(CompletionHandler<Integer, ? super A> ch) {
			super();
			this.ch = ch;
		}

		public void completed(Integer result, A attachment) {
			writePosition += result;
			ch.completed(result, attachment);
		}

		public void failed(Throwable exc, A attachment) {
			ch.failed(exc, attachment);
		}
	}; 
	
	public <A> void read(ByteBuffer dst, A attachment,
			CompletionHandler<Integer, ? super A> handler) {
		fileChannel.read(dst, readPosition, attachment, new ReadCompletionHandler<A>(handler));
	}
	public Future<Integer> read(ByteBuffer dst) {
		throw new UnsupportedOperationException("Not implemented");
	}
	public <A> void write(ByteBuffer src, A attachment,
			CompletionHandler<Integer, ? super A> handler) {
		fileChannel.write(src, writePosition, attachment, new WriteCompletionHandler<A>(handler));
	}
	public Future<Integer> write(ByteBuffer src) {
		throw new UnsupportedOperationException("Not implemented");
	}

	public void close() throws IOException {
		fileChannel.close();
	}
	public boolean isOpen() {
		return fileChannel.isOpen();
	}

}
