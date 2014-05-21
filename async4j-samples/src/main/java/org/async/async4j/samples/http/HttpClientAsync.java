package org.async.async4j.samples.http;

import static java.lang.String.format;
import static org.async4j.Async.asyncTry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousByteChannel;
import java.nio.channels.AsynchronousChannel;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.io.IOUtils;
import org.async4j.Callback;
import org.async4j.FunctionAsync;

public class HttpClientAsync {
	private final HeaderParser headerParser = new HeaderParser();
	private final AsynchronousChannelGroup asynchronousChannelGroup;
	// The local http montior for debug
	private final Map<String, ProxyCfg> proxyMap = new HashMap<>();
//	{
//		proxyMap.put("www.reuters.com", new ProxyCfg("localhost", 8081));
//		proxyMap.put("feeds.reuters.com", new ProxyCfg("localhost", 8082));
//	}

	public HttpClientAsync(AsynchronousChannelGroup asynchronousChannelGroup){
		this.asynchronousChannelGroup  =  asynchronousChannelGroup;
//		try {
//			 = AsynchronousChannelGroup.withFixedThreadPool(1, new ThreadFactory() {
//				public Thread newThread(Runnable arg0) {
//					Thread t = new Thread(arg0);
//					t.setDaemon(true);
////					t.start();
//					return t;
//				}
//			});
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
	}
	public void execute(final Callback<HttpResp> k, URI uri, final HttpReq req) {
		try {
			System.out.println("Loading "+uri);
			String host = uri.getHost();
			int port = uri.getPort();
			if (port == -1) {
				port = 80;
			}
			execute(k, host, port, req);
		} catch (Throwable e) {
			k.error(e);
		}
	}

	public void execute(final Callback<HttpResp> k, String host, int port, final HttpReq req) {
		try {
			final AsynchronousSocketChannel abc = AsynchronousSocketChannel.open(asynchronousChannelGroup);
//			final SocketAddress address = new InetSocketAddress("localhost", port);
			final SocketAddress address;
			
			ProxyCfg proxyCfg = proxyMap.get(host);
			if(proxyCfg == null){
				address= new InetSocketAddress(host, port);
			}else{
				address= new InetSocketAddress(proxyCfg.getHost(), proxyCfg.getPort());
			}

			asyncTry(k, null, new FunctionAsync<Void, HttpResp>() {
				public void apply(final Callback<? super HttpResp> k, Void p) {
					
					abc.connect(address, null, new CompletionHandler<Void, Void>() {
						@Override
						public void completed(Void arg0, Void arg1) {
							sendReq(k, abc, req);
						}

						@Override
						public void failed(Throwable e, Void arg1) {
							k.error(e);
						}
					});

				}
			})._asyncCatch(new FunctionAsync<Throwable, HttpResp>() {
				public void apply(Callback<? super HttpResp> k, Throwable p) {
					try {
						// When a error occured before response is return asynchronously to the caller
						// the channel has to be closed before exception is bubbled up 
						IOUtils.closeQuietly(abc);
						
						k.error(p);
					} catch (Throwable e) {
						k.error(e);
					}
				}
			});
		} catch (Throwable e) {
			k.error(e);
		}
	}

	protected void sendReq(final Callback<? super HttpResp> k, final AsynchronousSocketChannel abc, final HttpReq req) {
		try {
			StringBuilder sb = new StringBuilder();
			sb.append(format("%s %s HTTP/1.1\r\n", req.getMethod(), req.getUri()));
//			sb.append("Accept-Encoding: gzip, deflate\r\n");
//			System.out.println(sb);
			for (Map.Entry<String, String> header : req.getHeaders().entrySet()) {
				sb.append(format("%s: %s\r\n", header.getKey(), header.getValue()));
			}
			sb.append("\r\n");

			final ByteBuffer buffer = ByteBuffer.wrap(sb.toString().getBytes("UTF-8"));
			abc.write(buffer, null, new CompletionHandler<Integer, Void>() {
				@Override
				public void completed(Integer result, Void attachment) {
					if (buffer.remaining() > 0) {
						abc.write(buffer, null, this);
					} else {
						readResp(k, abc, req);
					}
				}

				@Override
				public void failed(Throwable exc, Void attachment) {
					k.error(exc);
				}
			});
		} catch (Throwable e) {
			k.error(e);
		}
	}

	protected void readResp(final Callback<? super HttpResp> k, final AsynchronousSocketChannel abc, HttpReq req) {
		try {
			final ByteBuffer buf = ByteBuffer.allocate(2024);
			buf.mark();
			final HeaderEndMatcher matcher = new HeaderEndMatcher();

			abc.read(buf, null, new CompletionHandler<Integer, Void>() {
				@Override
				public void completed(Integer result, Void attachment) {
					try {
						int positionBkp = buf.position();
						buf.reset();
						buf.limit(positionBkp);
						while (buf.remaining() > 0) {
							if (matcher.matched(buf.get())) {
								break;
							}
						}

						if (matcher.matched()) {
							byte[] headerBytes = new byte[buf.position()];
							if (buf.hasArray()) {
								System.arraycopy(buf.array(), buf.arrayOffset(), headerBytes, 0, headerBytes.length);
							} else {
								for (int i = 0; i < headerBytes.length; i++) {
									headerBytes[i] = buf.get(i);
								}
							}

							Header header = headerParser.parse(headerBytes);
							//System.out.println(header);
							InputChannelAsync input = new BufferedInputChannelAsync(buf, abc);
//							input = new LogInputChannelAsync(input);
							String contentEncoding = header.getFieldValue("Transfer-Encoding");
							if(contentEncoding != null && "chunked".equals(contentEncoding.trim())){
								input = new ChunckedInputChanelAsync(input);
							}
							HttpResp resp = new HttpResp(header, input);

							k.completed(resp);
						} else {
							if (result.intValue() == -1) {
								throw new HttpProtocolException("Reach response end before header completion");
							} else {
								buf.position(positionBkp);
								buf.limit(buf.capacity());
								if (buf.hasRemaining()) {
									buf.mark();
									abc.read(buf, null, this);
								} else {
									throw new HttpProtocolException("Header exceed size limit: > " + buf.capacity());
								}
							}
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
		} catch (Throwable e) {
			k.error(e);
		}
	}

	public static class HeaderEndMatcher {
		public static final int CR = 0x0D;
		public static final int LF = 0x0A;
		private int state;

		public boolean matched() {
			return state == 4;
		}

		public boolean matched(byte b) {
			switch (state) {
			case 0:
				state = b == CR ? 1 : 0;
				break;
			case 1:
				state = b == LF ? 2 : 0;
				break;
			case 2:
				state = b == CR ? 3 : 0;
				break;
			case 3:
				state = b == LF ? 4 : 0;
				break;
			default:
				state = 0;
				break;
			}

			return state == 4;
		}
	}
}
