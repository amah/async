package org.async.async4j.samples.gigasort;

import static org.async4j.Async.asyncParallelFor;
import static org.async4j.Async.asyncTry;
import static org.async4j.Async.withPool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.async.async4j.samples.Cmd;
import org.async.async4j.samples.gigasort.pool.ByteBufferPool;
import org.async.async4j.samples.gigasort.pool.CachingBufferPool;
import org.async4j.Block;
import org.async4j.Callback;
import org.async4j.FunctionAsync;
import org.async4j.FutureCallback;
import org.async4j.flow.StagingFlowController;
import org.async4j.nio.AsynchronousFileByteChannel;
import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.kohsuke.args4j.Option;

public class GigasortCmd implements Cmd , GigasortConstants{
	@Option(name="-f", required=false, usage="The file to sort")
	private File inputFile;
	@Option(name="-o", required=true, usage="Sorted output file")
	private File outputFile;
	@Option(name="-wd", required=true, usage="Work directory")
	private File workDir;
	@Option(name="-bs", required=false, usage="Single buffer size")
	private int bufferSize = 100*1000000;
	@Option(name="-bks", required=false, usage="Bucket size")
	private int bucketSize = 30*1000000;
	@Option(name="-smaplesz", required=false, usage="Sample size")
	private int sampleCount = 10000;
	@Option(name="-pc", required=false, usage="Bucket size")
	private int partitionCount = 10;
	@Option(name="-bc", required=false, usage="Number of buffers")
	private int bufferCount;
	@Option(name="-ps", required=false, usage="Pool size")
	private int poolSize = Runtime.getRuntime().availableProcessors() * 4;
	@Option(name="-split", required=false, usage="Split only input file")
	private boolean splitOnly = false;
	@Option(name="-merge", required=false, usage="Merge all files in work directory")
	private boolean mergeOnly = false;
	@Option(name="-splitbs", required=false, usage="Split input buffer size")
	private int splitBufferSize = 300*100000;
	@Option(name="-sortbs", required=false, usage="Sorted output buffer size")
	private int sortedOutputBufferSize = 100*1000000;
	
	final AtomicInteger splitSeq = new AtomicInteger();
	final ExecutorService pool = Executors.newFixedThreadPool(poolSize);

	final ConcurrentLinkedQueue<File> partitions = new ConcurrentLinkedQueue<>();

	private final AtomicLong byteCount = new AtomicLong();
	private PartitionTrie partitionTrie;
	
	@Override
	public void execute() throws Exception {
//		headOfBuckets();
		ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1);
		scheduledThreadPool.scheduleAtFixedRate(
			new Runnable() {
				public void run() {
					Runtime runtime = Runtime.getRuntime();
					System.out.println("Memory: "+(runtime.maxMemory() - runtime.freeMemory()));
				}
			}, 1, 1, TimeUnit.SECONDS);
		try {
			if(! mergeOnly){
				partitionTrie = sample(sampleCount, partitionCount, inputFile);
				Split split = SimonManager.getStopwatch("partition").start();
					FutureCallback<Void> k = new FutureCallback<>();
//					partition(k);
					bucketize(k);
					k.getResult();
					split.stop();
			}
			if(!splitOnly){
				Split split = SimonManager.getStopwatch("merge").start();
				outputFile.getParentFile().mkdirs();
				FutureCallback<Void> k = new FutureCallback<>();
//				List<File> files;
//				if(mergeOnly){
//					files=Arrays.asList(workDir.listFiles());
//				}
//				else{
//					files = new ArrayList<>(partitions);
//				}
//				merge(k, files, outputFile);
				
				partitionMerge(k, workDir, outputFile);
				k.getResult();
				split.stop();
			}
		} finally {
			pool.shutdown();
			scheduledThreadPool.shutdownNow();
		}

		System.out.println("End -->>"+byteCount);
		for (String name : SimonManager.getSimonNames()) {
			System.out.println(name +": "+SimonManager.getSimon(name));
		}
	}

	private void partition(FutureCallback<Void> k) throws IOException {
		if(! workDir.exists()){
			workDir.mkdirs();
		}

		asyncParallelFor(k, new PartitionProducerAsync(inputFile, bufferCount, bufferSize, pool), 2, new FunctionAsync<ByteBuffer, Void>() {
			public void apply(Callback<? super Void> k, ByteBuffer sortedBuffer) {
				try{
					writeSplitBuffer(k, sortedBuffer);
				}catch(Throwable e){k.error(e);}
			}
		});
	}

	

	private void bucketize(FutureCallback<Void> k) throws IOException {
		if(! workDir.exists()){
			workDir.mkdirs();
		}

		for (File partitionDir : workDir.listFiles()) {
			System.out.println("Deleting "+partitionDir);
			FileUtils.deleteDirectory(partitionDir);
		}
		
		final List<File> partitionDirList = new ArrayList<>(partitionCount);
		final List<AtomicInteger> partitionSeqList = new ArrayList<>(partitionCount);
		
		final AtomicReferenceArray<AsynchronousByteChannel> channels = new AtomicReferenceArray<>(partitionCount);
		for (int i = 0; i < channels.length(); i++) {
			File partitionDir = new File(workDir, String.format("%s-partition-%05d", FilenameUtils.getBaseName(inputFile.getName()), i));
			partitionDir.mkdirs();
			partitionDirList.add(partitionDir);
			partitionSeqList.add(new AtomicInteger());
		}
		
		final AtomicInteger writenCount = new AtomicInteger();
		
		asyncTry(k, null, new FunctionAsync<Void, Void>() {
			public void apply(Callback<? super Void> k, Void v) {
				final StagingFlowController<Void, Void> outputCtrl = new StagingFlowController<>(5);

				BucketProducerAsync producer = new BucketProducerAsync(inputFile, partitionTrie, bufferSize, bucketSize, pool);
				
				asyncParallelFor(k, new BucketStagingProducerAsync(producer), 10, withPool(pool, new FunctionAsync<Bucket, Void>() {
					public void apply(final Callback<? super Void> k, final Bucket bucket) {
						try{
							
							
							bucket.getBuffer().rewind();
							final ByteBuffer sortedBucketBuffer = sort(bucket.getBuffer());
							sortedBucketBuffer.rewind();
							
							//System.out.println(GigasortUtil.entryToHex(sortedBucketBuffer, 0)+" "+bucket);
							File partitionDir = partitionDirList.get(bucket.getPartitionId());
							File bucketFile = new File(partitionDir, String.format("%s-%d.%s", partitionDir.getName(), partitionSeqList.get(bucket.getPartitionId()).incrementAndGet(), FilenameUtils.getExtension(inputFile.getName())));
							final Path bucketPath = Paths.get(bucketFile.toURI());
							
							outputCtrl.run(k, null, new FunctionAsync<Void, Void>() {
								public void apply(final Callback<? super Void> k, Void p) {
									GigasortUtil.writeBytesToFile(k, sortedBucketBuffer, bucketPath);
								}
							});
						}catch(Throwable e){k.error(e);}
					}
				}));
			}
		}).asyncFinally(new Block() {
			public void apply() {
				for (int i = 0; i < channels.length(); i++) {
					IOUtils.closeQuietly(channels.get(i));
				}
				System.out.println("-WRITTEN-> "+writenCount);
			}
		});;
	}

	public ByteBuffer sort(ByteBuffer buffer){
		int entryCount = buffer.limit() / ENTRY_SIZE;
		Integer[] indexes = new Integer[entryCount];
		for (int i = 0; i < indexes.length; i++) {
			indexes[i] = i;
		}
		
		Arrays.sort(indexes, new EntryIndexComparator(buffer));
		
		byte[] src = buffer.array();
		byte[] dst = new byte[buffer.limit()];
		for (int i = 0; i < indexes.length; i++) {
			Integer index = indexes[i];
			System.arraycopy(src, index * 100, dst, i * 100, ENTRY_SIZE);
		}
		
		return ByteBuffer.wrap(dst);
	}
	

	private void writeSplitBuffer(final Callback<? super Void> k, final ByteBuffer sortedBuffer)
			throws IOException {
		try{
		sortedBuffer.rewind();

		File splitFile = new File(workDir, String.format("%s-split-%d.%s", FilenameUtils.getBaseName(inputFile.getName()), splitSeq.getAndIncrement(), FilenameUtils.getExtension(inputFile.getName())));
		partitions.add(splitFile);
		
		Path outputPath = Paths.get(splitFile.toURI());
		final AsynchronousFileChannel outputChannel = AsynchronousFileChannel.open(outputPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
	
		asyncTry(k, null, new FunctionAsync<Integer, Void>() {
			public void apply(final Callback<? super Void> k, Integer p) {
				try{
					System.err.println("-");
					
					outputChannel.write(sortedBuffer, 0L, 0L, new CompletionHandler<Integer, Long>() {
						public void completed(Integer writen, Long oldPos) {
							System.err.println("+");
							if(sortedBuffer.remaining() > 0){
								System.err.println("+*");
								long pos = oldPos + writen;
								outputChannel.write(sortedBuffer, pos, pos, this);
							}
							else{
								k.completed(null);
							}
						}

						public void failed(Throwable e, Long oldPos) {
							System.err.println("+!");
							k.error(e);
						}
					});
					
				}catch(Throwable e){
					k.error(e);
				}
			}
		}).asyncFinally(new Block() {
			public void apply() {
				IOUtils.closeQuietly(outputChannel);
			}
		});
		}catch(Throwable e){
			k.error(e);
		}
	}

	public boolean checkSort(ByteBuffer buffer){
		int entryCount = buffer.limit() / ENTRY_SIZE;
		Comparator<Integer> c = new EntryIndexComparator(buffer);
		for (int i = 1; i < entryCount; i++) {
			if(c.compare(i-1, i) > 0){
				return false;
			}
		}
		
		return true;
	}

	public static class EntryIndexComparator implements Comparator<Integer>, GigasortConstants{
		private final ByteBuffer buffer;
		
		public EntryIndexComparator(ByteBuffer buffer) {
			this.buffer = buffer;
		}

		@Override
		public int compare(Integer o1, Integer o2) {
			return GigasortUtil.compare(buffer, o1, buffer, o2);
		}
	}
	

	private void merge(Callback<? super Void> k, final List<File> inputFileList, File outputFile){
		try{
			Path outputPath = Paths.get(outputFile.toURI());
			AsynchronousFileChannel outputChannel = AsynchronousFileChannel.open(outputPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
			final AsynchronousByteChannel abc = new AsynchronousFileByteChannel(outputChannel);
			
			partitionMergeSort(k, inputFileList, abc);
			
		}catch(Throwable e){
			k.completed(null);
		}
		
	}


	public void partitionMerge(Callback<? super Void> k, final File workDir, final File outputFile){
		try{
			FileUtils.writeByteArrayToFile(outputFile, new byte[]{});
			File[] files = workDir.listFiles();
			Arrays.sort(files);
			for (File file : files) {
				System.out.println(file);
			}
			List<File> partitionDirList = Arrays.asList(files);
			
			final Map<File, Long> partitionPosMap = new HashMap<File, Long>();
			long position = 0;
			for (File partitionDir : partitionDirList) {
				partitionPosMap.put(partitionDir, position);
				position += FileUtils.sizeOf(partitionDir);
			}
			
			asyncParallelFor(k, partitionDirList, 3, new FunctionAsync<File, Void>() {
				public void apply(Callback<? super Void> k, final File partitionDir) {
					try{
						Path outputPath = Paths.get(outputFile.toURI());
						AsynchronousFileChannel outputChannel = AsynchronousFileChannel.open(outputPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
						final AsynchronousByteChannel abc = new AsynchronousFileByteChannel(outputChannel, partitionPosMap.get(partitionDir));

						asyncTry(k, null, new FunctionAsync<Void, Void>() {
							public void apply(final Callback<? super Void> k, Void p) {
								List<File> bucketFileList = Arrays.asList(partitionDir.listFiles());
								partitionMergeSort(k, bucketFileList, abc);
							}
						}).asyncFinally(new Block() {
							public void apply() {
								IOUtils.closeQuietly(abc);
							}
						});;
					}catch(Throwable e){k.error(e);}
				}
			});
			
		}catch(Throwable e){
			k.completed(null);
		}
		
	}

	private void partitionMergeSort(Callback<? super Void> k, final List<File> inputFileList,
			final AsynchronousByteChannel abc) {
		asyncParallelFor(k, new SortedBufferProducerAsync(byteCount, inputFileList, splitBufferSize, sortedOutputBufferSize, pool), 2, withPool(pool, new FunctionAsync<ByteBuffer, Void>() {
			public void apply(final Callback<? super Void> k, final ByteBuffer buffer) {
				try{
					buffer.rewind();
//					System.out.println(GigasortUtil.entryToHex(buffer, 0));
//					byteCount.addAndGet(buffer.limit());

					final Split split = SimonManager.getStopwatch("mergesort.write").start();
					abc.write(buffer, null, new CompletionHandler<Integer, Void>() {
						public void completed(Integer writen, Void v) {
							byteCount.addAndGet(writen);
							if(buffer.remaining() > 0){
								abc.write(buffer, null, this);
							}
							else{
								split.stop();
								k.completed(null);
							}
						}

						public void failed(Throwable e, Void v) {
							k.error(e);
						}
					});
				}catch(Throwable e){
					k.error(e);
				}
			}
		}));
	}
	
	

	public PartitionTrie sample(int sampleCount, int partitionCount, File inputFile) throws IOException {
		Chrono chrono = new Chrono();
		System.out.print("Sampling ... ");
		RandomAccessFile raf = new RandomAccessFile(inputFile, "r");
		try{
			ByteBuffer sampleBuffer = ByteBuffer.allocate(sampleCount * ENTRY_SIZE);
			raf.getChannel().read(sampleBuffer);
			ByteBuffer sortedBuffer = sort(sampleBuffer);
			
			int step  = (sampleCount / partitionCount) * ENTRY_SIZE;
			ByteBuffer trieBuffer = ByteBuffer.allocate((partitionCount - 1)* ENTRY_SIZE);
			while (trieBuffer.remaining() > 0) {
				GigasortUtil.copyEntry(sortedBuffer, trieBuffer);
				System.out.println(GigasortUtil.entryToHex(sortedBuffer, sortedBuffer.position() - ENTRY_SIZE));
				sortedBuffer.position(sortedBuffer.position()+step-ENTRY_SIZE);
			}
			
			trieBuffer.rewind();
			System.out.println("Trie buffer "+trieBuffer);
			return new PartitionTrie(trieBuffer, partitionCount);
		}finally{
			IOUtils.closeQuietly(raf);
			System.out.println("Done "+chrono.end());
		}
	}



	public void headOfBuckets() throws IOException {
		List<File> partitionDirList = Arrays.asList(workDir.listFiles());
		for (File partitionDir : partitionDirList) {
			List<File> bucketFileList = Arrays.asList(partitionDir.listFiles());

			for (File bucketFile : bucketFileList) {
				RandomAccessFile raf = new RandomAccessFile(inputFile, "r");
				try{
					ByteBuffer sampleBuffer = ByteBuffer.allocate(ENTRY_SIZE);
					raf.getChannel().read(sampleBuffer);
					System.out.printf("%s : %s\n", bucketFile.getAbsolutePath(), GigasortUtil.entryToHex(sampleBuffer, 0));
				}finally{
					IOUtils.closeQuietly(raf);
				}
				
			}
		}
	}
}
