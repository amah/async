package org.async4j.nio;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousByteChannel;
import java.nio.channels.CompletionHandler;

import org.async4j.Callback2;
import org.async4j.streams.EnumeratorAsync;

public class ByteChannelEnumeratorAsync implements EnumeratorAsync<ByteBuffer> {
	private final AsynchronousByteChannel channel;
	private final ByteBuffer buffer;
	private final ReadCompletionHandler completionHandler = new ReadCompletionHandler();

	public ByteChannelEnumeratorAsync(AsynchronousByteChannel channel,
			ByteBuffer buffer) {
		super();
		this.channel = channel;
		this.buffer = buffer;
	}

	public void next(final Callback2<Boolean, ? super ByteBuffer> k) {
		try {
			channel.read(buffer, k, completionHandler);
		} catch (Throwable e) {
			k.error(e);
		}
	}

	protected class ReadCompletionHandler implements
			CompletionHandler<Integer, Callback2<Boolean, ? super ByteBuffer>> {

		public void completed(Integer result, Callback2<Boolean, ? super ByteBuffer> k) {
			try {
				k.completed(result.intValue() != -1, buffer);
			} catch (Throwable e) {
				k.error(e);
			}
		}

		public void failed(Throwable exc, Callback2<Boolean, ? super ByteBuffer> k) {
			k.error(exc);
		}
	}
}
