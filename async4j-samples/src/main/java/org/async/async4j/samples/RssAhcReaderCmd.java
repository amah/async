package org.async.async4j.samples;

import static org.async4j.Async.asyncParallelFor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.async4j.Callback;
import org.async4j.FunctionAsync;
import org.async4j.FutureCallback;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

public class RssAhcReaderCmd implements Cmd {

	@Override
	public void execute() throws Exception {
		final AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
		Future<Response> f = asyncHttpClient.prepareGet("http://www.reuters.com/tools/rss").execute();
		
		String page = f.get().getResponseBody();

		Pattern pattern = Pattern.compile("<td class=\"feedUrl\">(.*?)</td>");
		Matcher matcher = pattern.matcher(page);
		
		List<String> feedUrlList = new ArrayList<>();
		while (matcher.find()) {
			feedUrlList.add(matcher.group(1));
		}

//		for (String url : feedUrlList) {
//			System.out.println(url);
//		}
		
//		feedUrlList.remove("http://feeds.feedburner.com/reuters/blogs/DavosNotebook");
		final FutureCallback<Void> k = new FutureCallback<>();
		
		asyncParallelFor(k, feedUrlList, 30, new FunctionAsync<String, Void>() {
			public void apply(final Callback<? super Void> k, final String feedUrl) {
				try{
					 asyncHttpClient.prepareGet(feedUrl).execute(
							   new AsyncCompletionHandler<Integer>(){

							    @Override
							    public Integer onCompleted(Response response) throws Exception{
							    	response.getResponseBody();
//							    	System.out.println(Thread.currentThread().getThreadGroup().activeCount());
							    	k.completed(null);
							        return response.getStatusCode();
							    }

							    @Override
							    public void onThrowable(Throwable t){
							    	k.completed(null);
							    }
							});
				}catch(Throwable e){
					k.error(e);
				}
			}
		});

		k.getResult();
		asyncHttpClient.close();
	}

}
