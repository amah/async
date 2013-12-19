package org.async.async4j.samples;

import static org.async4j.Async.asyncParallelFor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;

import org.async4j.Callback;
import org.async4j.FunctionAsync;
import org.async4j.FutureCallback;
import org.glassfish.jersey.client.ClientProperties;

public class RssReaderCmd implements Cmd {

	@Override
	public void execute() throws Exception {
		final Client c = ClientBuilder.newClient();
		c.property(ClientProperties.ASYNC_THREADPOOL_SIZE, 10);
		String uri = "http://www.reuters.com/tools/rss";
		final WebTarget target = c.target(uri);
		String page = target.path("/").request().get(String.class);

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
		
		asyncParallelFor(k, feedUrlList, 10, new FunctionAsync<String, Void>() {
			public void apply(final Callback<? super Void> k, final String feedUrl) {

				WebTarget target = c.target(feedUrl);
				target.request().async().get(new InvocationCallback<String>() {
					public void completed(String response) {
//						System.out.println(response);
//						System.out.println(Thread.currentThread());
						k.completed(null);
					}
					public void failed(Throwable throwable) {
//						System.err.println("Failed for: "+feedUrl);
						k.completed(null); // Ignoring the exception
					}
				});
			}
		});

		k.getResult();
		c.close();
	}

}
