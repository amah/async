package org.async.async4j.samples;

import static org.async4j.Async.asyncParallelFor;
import static org.async4j.Async.withPool;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;
import org.async.async4j.samples.http.HttpClientAsync;
import org.async.async4j.samples.http.HttpReq;
import org.async.async4j.samples.http.HttpResp;
import org.async4j.Callback;
import org.async4j.FunctionAsync;
import org.async4j.FutureCallback;

public class RssNioReaderCmd implements Cmd {

	@Override
	public void execute() throws Exception {

		final ExecutorService threadPool = Executors.newFixedThreadPool(1);
		final HttpClientAsync httpClient = new HttpClientAsync(AsynchronousChannelGroup.withThreadPool(threadPool));
		try {
			
			String page = requestInitialPage(httpClient, "http://www.reuters.com/tools/rss");
			Pattern pattern = Pattern.compile("<td class=\"feedUrl\">(.*?)</td>");
			Matcher matcher = pattern.matcher(page);

			List<String> feedUrlList = new ArrayList<>();
			while (matcher.find()) {
				feedUrlList.add(matcher.group(1));
			}

//			for (String url : feedUrlList) {
//				System.out.println(url);
//			}
			System.out.println("Size "+feedUrlList.size());
			final FutureCallback<Void> k = new FutureCallback<>();

			asyncParallelFor(k, feedUrlList, 100, withPool(threadPool, new FunctionAsync<String, Void>() {
				public void apply(final Callback<? super Void> k, final String feedUrl) {
					try{
						Callback<HttpResp> httpK = new Callback<HttpResp>() {
							public void completed(HttpResp result) {
								result.getResponseBodyAsString(new Callback<String>() {
									public void completed(String result) {
										k.completed(null);
									}
									public void error(Throwable e) {
										k.error(e);
									}
								});
							}
							public void error(Throwable e) {
								k.error(e);
							}
						};
						httpClient.execute(httpK, new URI(feedUrl), HttpReq.get(new URI(feedUrl)));
					}catch(Throwable t){k.error(t);}
				}
			}));

			k.getResult();

		} finally {
			threadPool.shutdownNow();
		}
	}

	private String requestInitialPage(HttpClientAsync httpclient, String uri) throws URISyntaxException, ClientProtocolException, IOException {
		final FutureCallback<HttpResp> k = new FutureCallback<>();
		
		httpclient.execute(k, new URI(uri), HttpReq.get(new URI(uri)));

		HttpResp resp = k.getResult();
		return responseToText(resp);
	}


	public static String responseToText(HttpResp response) throws IOException, ClientProtocolException {
		System.out.println(response.getHeader());
		int status = response.getHeader().getStatusLine().getStatusCode();
		if (status >= 200 && status < 300) {
			final FutureCallback<String> k = new FutureCallback<>();
			response.getResponseBodyAsString(k);
			return k.getResult();
		} else {
			throw new ClientProtocolException("Unexpected response status: " + status);
		}
	}

}
