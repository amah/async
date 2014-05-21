package org.async.async4j.samples.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousByteChannel;
import java.nio.channels.CompletionHandler;

public class BufferedInputChannelAsync implements InputChannelAsync {
	private final ByteBuffer buffer;
	private final AsynchronousByteChannel abc;

	public BufferedInputChannelAsync(ByteBuffer buffer, AsynchronousByteChannel abc) {
		this.buffer = buffer;
		this.abc = abc;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.async.async4j.samples.http.InputChannelAsync#close()
	 */
	@Override
	public void close() throws IOException {
		abc.close();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.async.async4j.samples.http.InputChannelAsync#isOpen()
	 */
	@Override
	public boolean isOpen() {
		return abc.isOpen();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.async.async4j.samples.http.InputChannelAsync#read(java.nio.ByteBuffer
	 * , A, java.nio.channels.CompletionHandler)
	 */
	@Override
	public <A> void read(final ByteBuffer dst, A attachment, final CompletionHandler<Integer, ? super A> handler) {
		try {
			if (!dst.hasRemaining()) {
				handler.completed(0, attachment);
			} else {
				if (buffer.hasRemaining()) {
					int n = Math.min(dst.remaining(), buffer.remaining());
					dst.put(buffer.array(), buffer.arrayOffset() + buffer.position(), n);
					buffer.position(buffer.arrayOffset() + buffer.position() + n);

					handler.completed(n, attachment);
				} else {
					abc.read(dst, attachment, handler);
				}
			}
		} catch (Throwable e) {
			handler.failed(e, attachment);
		}
	}
}
