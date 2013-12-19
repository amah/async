package org.async.async4j.samples.gigasort;

import static org.async4j.Async.asyncTry;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousByteChannel;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.async.async4j.samples.gigasort.MergeSortAggregator.ConsumerCallback;
import org.async.async4j.samples.gigasort.MergeSortAggregator.MergeSortItem;
import org.async4j.Block;
import org.async4j.Callback;
import org.async4j.FunctionAsync;
import org.async4j.nio.AsynchronousFileByteChannel;

public class GigasortUtil implements GigasortConstants{
	public static int copy(ByteBuffer src, ByteBuffer dst){
		int n = Math.min(src.remaining(), dst.remaining());
		
		System.arraycopy(src.array(), src.position(), dst.array(), dst.position(), n);
		src.position(src.position() + n);
		dst.position(dst.position() + n);
		return n;
	}
	public static int transfer(ByteBuffer src, ByteBuffer dst){
		int n = src.limit();
		
		System.arraycopy(src.array(), src.position(), dst.array(), dst.position(), n);
		src.position(src.position() + n);
		dst.position(dst.position() + n);
		return n;
	}
	public static void copyEntry(ByteBuffer src, ByteBuffer dst){
		System.arraycopy(src.array(), src.position(), dst.array(), dst.position(), ENTRY_SIZE);
		src.position(src.position() + ENTRY_SIZE);
		dst.position(dst.position() + ENTRY_SIZE);
	}
	public static int compare(ByteBuffer b1, Integer o1, ByteBuffer b2, Integer o2) {
		int offset1 = o1 * ENTRY_SIZE;
		int offset2 = o2 * ENTRY_SIZE;
		return compareOffset(b1, offset1, b2, offset2);
	}
	public static int compareOffset(ByteBuffer b1, int offset1, ByteBuffer b2, int offset2) {
		for(int i = 0; i < KEY_SIZE; i++){
			int value1 = b1.get(offset1++) & 0xFF;
			int value2 = b2.get(offset2++) & 0xFF;
			if(value1 < value2){
				return -1;
			}
			else if (value1 > value2){
				return 1;
			}
		}
		
		return 0;
	}

	public static <E> E[] atomicArrayCopy(AtomicReferenceArray<E> atomicArray, E[] plainArray){
		for (int i = 0; i < plainArray.length; i++) {
			plainArray[i] = atomicArray.get(i);
		}
		return plainArray;
	}


	public static <E> void atomicArrayUpdate(AtomicReferenceArray<E> atomicArray, E[] plainArray){
		for (int i = 0; i < plainArray.length; i++) {
			atomicArray.set(i,plainArray[i]);
		}
	}
	
	public static void writeBytesToFile(final Callback<? super Void> k, final ByteBuffer buffer, Path file){
		try{
			AsynchronousFileChannel outputChannel = AsynchronousFileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
			final AsynchronousByteChannel channel =  new AsynchronousFileByteChannel(outputChannel);

			asyncTry(k, null, new FunctionAsync<Void, Void>() {
				public void apply(final Callback<? super Void> k, Void v) {

					channel.write(buffer, null, new CompletionHandler<Integer, Void>() {

						public void completed(Integer result, Void attachment) {
							if(buffer.remaining() > 0){
								channel.write(buffer, null, this);
							}
							else{
								k.completed(null);
							}
						}

						public void failed(Throwable exc, Void attachment) {
							k.error(exc);
						}
					});
				}
			}).asyncFinally(new Block() {
				public void apply() {
					IOUtils.closeQuietly(channel);
				}
			});
		}catch(Throwable e){
			k.error(e);
		}
	}

	public static String entryToHex(ByteBuffer buffer, int offset){
		byte[] b = new byte[10];
		for (int i = 0; i < b.length; i++) {
			b[i] = buffer.get(offset + i);
		}
		
		return Hex.encodeHexString(b);
	}
	
	public static void checkPartitionBuffer(ByteBuffer buffer, int offset, PartitionTrie partitionTrie, int expectedPartition){
		while(offset < buffer.position()){
			if(partitionTrie.partitionOf(buffer, offset) != expectedPartition){
				System.out.printf("###Entry %s not in right partition: %s\n", GigasortUtil.entryToHex(buffer, offset), expectedPartition);
			}
			offset+=GigasortConstants.ENTRY_SIZE;
		}
	}
	
	public static void copyToArray(ByteBuffer buffer, int offset, byte[] array){
		for (int i = 0; i < array.length; i++) {
			array[i] = buffer.get(offset + i);
		}
	}
}
