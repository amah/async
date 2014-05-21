package org.async.async4j.samples.http;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

public interface InputChannelAsync extends Closeable{

	public abstract void close() throws IOException;

	public abstract boolean isOpen();

	public abstract <A> void read(ByteBuffer dst, A attachment, CompletionHandler<Integer, ? super A> handler);

}