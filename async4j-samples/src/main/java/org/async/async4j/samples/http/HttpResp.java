package org.async.async4j.samples.http;

import static org.async4j.Async.asyncTry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;
import org.async4j.Block;
import org.async4j.Callback;
import org.async4j.FunctionAsync;

public class HttpResp {
	private final Header header;
	private final InputChannelAsync input;

	public HttpResp(Header header, InputChannelAsync input) {
		this.header = header;
		this.input = input;
	}

	public Header getHeader() {
		return header;
	}

	public InputChannelAsync getInput() {
		return input;
	}

	public void getResponseBodyAsBytes(final Callback<byte[]> k) {
		if (!input.isOpen()) {
			k.error(new RuntimeException("Response body already read"));
		} else {
			asyncTry(k, null, new FunctionAsync<Void, byte[]>() {
				public void apply(final Callback<? super byte[]> k, Void p) {
					final ByteArrayOutputStream baos = new ByteArrayOutputStream();
					final ByteBuffer buffer = ByteBuffer.allocate(1024);
					final Long contentLength = header.getFieldValueAsLong("Content-Length");
					input.read(buffer, null, new CompletionHandler<Integer, Void>() {
						@Override
						public void completed(Integer result, Void attachment) {
							try {
								if (result.intValue() != -1) {

									baos.write(buffer.array(), 0, buffer.position());
									buffer.rewind();
									if(contentLength != null){
										if (baos.size() < contentLength.longValue()) {
											input.read(buffer, null, this);
										} else {
											k.completed(uncompress(baos.toByteArray()));
										}
									}
									else{
										input.read(buffer, null, this);
									}
								} else {
									k.completed(uncompress(baos.toByteArray()));
								}
							} catch (Throwable e) {
								k.error(e);
							}
						}

						@Override
						public void failed(Throwable exc, Void attachment) {
							k.error(exc);
						}
					});
				}
			}).asyncFinally(new Block() {
				public void apply() {
					IOUtils.closeQuietly(input);
				}
			});
		}
	}

	public void getResponseBodyAsString(final Callback<String> k) {
		try {
			getResponseBodyAsBytes(new Callback<byte[]>() {
				@Override
				public void completed(byte[] bytes) {
					try {
						k.completed(new String(bytes));
					} catch (Throwable e) {
						k.error(e);
					}
				}

				@Override
				public void error(Throwable e) {
					k.error(e);
				}
			});
		} catch (Throwable e) {
			k.error(e);
		}
	}

	protected byte[] uncompress(byte[] in) throws IOException {
		if("gzip".equals(header.getFieldValue("Content-Encoding"))){
			GZIPInputStream zIn = new GZIPInputStream(new ByteArrayInputStream(in));
			ByteArrayOutputStream baos = new ByteArrayOutputStream(in.length * 3);

			try {
				IOUtils.copy(zIn, baos);
			} finally {
				IOUtils.closeQuietly(zIn);
			}
			
			return baos.toByteArray();
		}
		else{
			return in;
		}
	}
}
