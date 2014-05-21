package org.async.async4j.samples.http;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.CompletionHandler;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.async.async4j.samples.http.ChunckedInputChanelAsync.ChunckHeaderMatcher;
import org.junit.Test;

public class ChunckedInputChanelAsyncTest {
	
	@Test
	public void matcherTest() throws IOException{
		ChunckHeaderMatcher matcher = new ChunckHeaderMatcher();
		byte[] bytes;
		int i;
		
		matcher.reset();
		bytes = "2\r\n".getBytes();
		i = match(bytes, matcher);
		Assert.assertTrue(matcher.matched());
		Assert.assertEquals(bytes.length - 1, i);

		matcher.reset();
		bytes = "\r\n2\r\n".getBytes();
		i = match(bytes, matcher);
		Assert.assertTrue(matcher.matched());
		Assert.assertEquals(bytes.length - 1, i);
		
		matcher.reset();
		bytes = "\r\n2\r\nabcd".getBytes();
		i = match(bytes, matcher);
		Assert.assertTrue(matcher.matched());
		Assert.assertEquals(4, i);
		
		matcher.reset();
		bytes = "\r\n234\r\nabcd".getBytes();
		i = match(bytes, matcher);
		Assert.assertTrue(matcher.matched());
		Assert.assertEquals(6, i);
		
	}
	private int match(byte[] bytes, ChunckHeaderMatcher matcher) {
		int i;
		for (i = 0; i < bytes.length; i++) {
			byte b = bytes[i];
			if(matcher.matched(b)){
				break;
			}
		}
		return i;
	}		
	@Test
	public void nominalChunkTest() throws IOException{
		byte[] bytes = FileUtils.readFileToByteArray(new File("src/test/data/chunked-text.txt"));
		
		final InputChannelAsync input = new BytePerByteAsynchronousChannel(bytes);
		final InputChannelAsync inputChannelAsync = new ChunckedInputChanelAsync(input);
		
		final ByteBuffer dst = ByteBuffer.allocate(bytes.length);
		dst.clear();
		
		final org.async4j.FutureCallback<Void> k = new org.async4j.FutureCallback();
		inputChannelAsync.read(dst, null, new CompletionHandler<Integer, Void>() {
			public void completed(Integer result, Void attachment) {
				if(result != -1){
//					System.out.println(dst);
					System.out.println(new String(dst.array(), 0, dst.position()));
					inputChannelAsync.read(dst, attachment, this);
				}
				else{
					k.completed(null);
				}
			}

			public void failed(Throwable exc, Void attachment) {
				k.error(exc);
			}
		});
		
		k.getResult();
		
	}
	
	static class BytePerByteAsynchronousChannel implements InputChannelAsync{
		volatile boolean opened = true;
		volatile int position;
		final byte[] bytes;
		
		
		public BytePerByteAsynchronousChannel(byte[] bytes) {
			this.bytes = bytes;
		}

		@Override
		public void close() throws IOException {
			opened = false;
		}

		@Override
		public boolean isOpen() {
			return opened;
		}

		@Override
		public <A> void read(ByteBuffer dst, A attachment, CompletionHandler<Integer, ? super A> handler) {
			try{
				if(! opened){
					throw new AsynchronousCloseException();
				}
				else{
					if(position < bytes.length){
						if(dst.hasRemaining()){
							dst.put(bytes[position++]);
							handler.completed(1, attachment);
						}
						else{
							handler.completed(0, attachment);
						}
					}
					else{
						handler.completed(-1, attachment);
					}
				}
			}catch(Throwable e){
				handler.failed(e, attachment);
			}
		}
	}
}
