package org.async.async4j.samples.http;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

import org.apache.commons.io.FileUtils;

public class LogInputChannelAsync implements InputChannelAsync{
	private final InputChannelAsync inputChannelAsync;
	private volatile ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
	public LogInputChannelAsync(InputChannelAsync inputChannelAsync) {
		super();
		this.inputChannelAsync = inputChannelAsync;
	}

	@Override
	public void close() throws IOException {
		inputChannelAsync.close();
		baos = null;
	}

	@Override
	public boolean isOpen() {
		return inputChannelAsync.isOpen();
	}

	@Override
	public <A> void read(final ByteBuffer dst, A attachment, final CompletionHandler<Integer, ? super A> handler) {
		final int pos = dst.position();
		inputChannelAsync.read(dst, attachment, new CompletionHandler<Integer, A>() {
			public void completed(Integer result, A attachment) {
//				String s = new String(dst.array(), pos, dst.position()-pos);
//				System.out.print(s);
				baos.write(dst.array(), pos, dst.position()-pos);
				handler.completed(result, attachment);
			}

			public void failed(Throwable exc, A attachment) {
				handler.failed(exc, attachment);
			}
		});
	}

	public void dump(){
//		System.out.println(baos.toString());
		try {
			FileUtils.writeByteArrayToFile(new File("target/dump.txt"), baos.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
