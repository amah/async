package org.async.async4j.samples.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

public class ChunckedInputChanelAsync implements InputChannelAsync {
	private InputChannelAsync inputChannelAsync;
	private final ByteBuffer chunckHeaderBuffer = ByteBuffer.allocate(32);
	private volatile int remaining = 0;
	private final ChunckHeaderMatcher chem = new ChunckHeaderMatcher();

	
	public ChunckedInputChanelAsync(InputChannelAsync inputChannelAsync) {
		this.inputChannelAsync = inputChannelAsync;
		chunckHeaderBuffer.flip();
	}

	public void close() throws IOException {
		inputChannelAsync.close();
	}

	public boolean isOpen() {
		return inputChannelAsync.isOpen();
	}

	public <A> void read(final ByteBuffer dst, A attachment, final CompletionHandler<Integer, ? super A> handler) {
		final int dstInitalPos = dst.position();
		chunckRead(dst, attachment, handler, dstInitalPos);
	}

	protected <A> void chunckRead(final ByteBuffer dst, A attachment,
			final CompletionHandler<Integer, ? super A> handler, final int dstInitalPos) {
		if(remaining == -1){
			handler.completed(-1, attachment);
			return;
		}

		if (dst.hasRemaining()) {
			int r = Math.min(chunckHeaderBuffer.remaining(), dst.remaining());
			for( int i = 0; i < r; i++){
				dst.put(chunckHeaderBuffer.get());
			}
			remaining -= r;

			if (!dst.hasRemaining()) {
				handler.completed(dst.position() - dstInitalPos, attachment);
			} else {
				if (remaining > 0) {
					final ByteBuffer actualDst = dst.slice();
					int actualLimit = Math.min(dst.remaining(), remaining);
					actualDst.limit(actualLimit);

					inputChannelAsync.read(actualDst, attachment, new CompletionHandler<Integer, A>() {
						public void completed(Integer result, A attachment) {
							if (result.intValue() > 0) {
								remaining -= result;
								dst.position(dst.position() + result);
								handler.completed(dst.position() - dstInitalPos, attachment);
							} else {
								if (dst.position() == dstInitalPos) {
									handler.completed(result, attachment);
								} else {
									handler.completed(dst.position() - dstInitalPos, attachment);
								}
							}
						}

						public void failed(Throwable exc, A attachment) {
							handler.failed(exc, attachment);
						}
					});
				} else {
					if(remaining == 0){
						readChunkHeader(dst, attachment, handler, dstInitalPos);
					}
					else{
						// remaining is -1
						handler.completed(-1, attachment);
					}
				}

			}
		} else {
			handler.completed(0, attachment);
		}
	}

	protected <A> void readChunkHeader(final ByteBuffer dst, A attachment,
			final CompletionHandler<Integer, ? super A> handler, final int dstInitalPos) {

		// if remaining bytes in the buffer, 
		// move them to the buffer beginning and then mark it
		chunckHeaderBuffer.compact();
		chunckHeaderBuffer.mark();
		// FIXME read remaining from chunk header buffer first
		inputChannelAsync.read(chunckHeaderBuffer, attachment, new CompletionHandler<Integer, A>() {
			public void completed(Integer result, A attachment) {
				try {
					if (result > 0) {
						int pos = chunckHeaderBuffer.position();
						chunckHeaderBuffer.reset();
						while (chunckHeaderBuffer.position() < pos) {
							if (chem.matched(chunckHeaderBuffer.get())) {
								break;
							}
						}

						if (chem.matched()) {
							// TODO handle trailers chunk header
							int shift = chem.matchedWithCrlfPrefix() ? 2 : 0;
							String chunkHeader = new String(chunckHeaderBuffer.array(), shift, chunckHeaderBuffer.position() - 2 - shift);
							remaining = Integer.parseInt(chunkHeader, 16);
							chem.reset();
							chunckHeaderBuffer.limit(pos);
							if(remaining > 0){
								chunckRead(dst, attachment, handler, dstInitalPos);
							}else{
								remaining = -1;
								final int r = pos - chunckHeaderBuffer.position();
								if(r < 2){
									chunckHeaderBuffer.rewind();
									chunckHeaderBuffer.limit(r);
									inputChannelAsync.read(chunckHeaderBuffer, attachment, new CompletionHandler<Integer, A>() {
										public void completed(Integer result, A attachment) {
											if(chunckHeaderBuffer.hasRemaining()){
												inputChannelAsync.read(chunckHeaderBuffer, attachment, this);
											}
											else{
												handler.completed(dst.position() - dstInitalPos, attachment);
											}
										}
										public void failed(Throwable exc, A attachment) {
											handler.failed(exc, attachment);
										}
									});
								}
								else{
									handler.completed(dst.position() - dstInitalPos, attachment);
								}
							}
							return;
						} else {
							if (chunckHeaderBuffer.hasRemaining()) {
								chunckHeaderBuffer.mark();
								inputChannelAsync.read(chunckHeaderBuffer, attachment, this);
								return;
							} else {
								System.out.println(new String(chunckHeaderBuffer.array(), 0, chunckHeaderBuffer.position()));
								String mesg = "Chunck header end not found, byte read: "
										+ chunckHeaderBuffer.position();
								System.out.println(inputChannelAsync.getClass());
								if (inputChannelAsync instanceof LogInputChannelAsync) {
									LogInputChannelAsync logInputChannelAsync = (LogInputChannelAsync) inputChannelAsync;
									logInputChannelAsync.dump();
								}
								throw new HttpProtocolException(mesg);
							}
						}
					}

				} catch (Throwable e) {
					handler.failed(e, attachment);
				}
			}

			public void failed(Throwable exc, A attachment) {
				handler.failed(exc, attachment);
				return;
			}
		});
	}

	public static class ChunckHeaderEndMatcher {
		public static final int CR = 0x0D;
		public static final int LF = 0x0A;
		private volatile int state;

		public boolean matched() {
			return state == 2;
		}

		public boolean matched(byte b) {
			switch (state) {
			case 0:
				state = b == CR ? 1 : 0;
				break;
			case 1:
				state = b == LF ? 2 : 0;
				break;
			default:
				state = 0;
				break;
			}

			return state == 2;
		}

		public void reset() {
			state = 0;
		}
	}



	public static class ChunckHeaderMatcher {
		public static final int CR = 0x0D;
		public static final int LF = 0x0A;
		private volatile int state;

		public boolean matched() {
			return state == 4 || state == 7;
		}

		public boolean matched(byte b) {
			int s = state;
			switch (s) {
			case 0:
				s = b == CR ? 1 : 5;
				break;
			case 1:
				s = b == LF ? 2 : -1;
				break;
			case 2:
				s = b == CR ? 3 : 2;
				break;
			case 3:
				s = b == LF ? 4 : 2;
				break;
			case 5:
				s = b == CR ? 6 : 5;
				break;
			case 6:
				s = b == LF ? 7 : 5;
				break;
			default:
				state = -1;
				break;
			}

			state = s;
			return s == 4 || s==7;
		}

		public void reset() {
			state = 0;
		}
		
		public boolean matchedWithCrlfPrefix(){
			return state == 4;
		}
	}
}
