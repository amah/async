package org.async.async4j.samples;

import static org.async4j.Async.asyncParallelFor;

import java.io.IOException;
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
import org.async4j.Callback;
import org.async4j.FunctionAsync;
import org.async4j.FutureCallback;

public class RssHcReaderCmd implements Cmd {

	@Override
	public void execute() throws Exception {
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(3000).setConnectTimeout(3000).build();
		final CloseableHttpAsyncClient httpclient = HttpAsyncClients.custom().setDefaultRequestConfig(requestConfig)
				.build();

		final ExecutorService threadPool = Executors.newFixedThreadPool(10);
		try {
			
			httpclient.start();

			String page = requestInitialPage(httpclient, "http://www.reuters.com/tools/rss");
			Pattern pattern = Pattern.compile("<td class=\"feedUrl\">(.*?)</td>");
			Matcher matcher = pattern.matcher(page);

			List<String> feedUrlList = new ArrayList<>();
			while (matcher.find()) {
				feedUrlList.add(matcher.group(1));
			}

			final FutureCallback<Void> k = new FutureCallback<>();

			asyncParallelFor(k, feedUrlList, 10, new FunctionAsync<String, Void>() {
				public void apply(final Callback<? super Void> k, final String feedUrl) {
					try{
						httpclient.execute(new HttpGet(feedUrl), new org.apache.http.concurrent.FutureCallback<HttpResponse>() {
							@Override
							public void completed(final HttpResponse response) {
								try {
									String s = responseToText(response);
									k.completed(null);
								}
								catch(ClientProtocolException e){
									System.out.println("Failed "+feedUrl);
									k.completed(null);
								}
								catch (Throwable t) {	k.error(t);	}
							}

							@Override
							public void cancelled() {
								k.completed(null);
							}

							@Override
							public void failed(Exception ex) {
								k.error(ex);
							}
						});
						
					}catch(Throwable t){k.error(t);}
				}
			});

			k.getResult();

		} finally {
			httpclient.close();
			threadPool.shutdownNow();
		}
	}

	private String requestInitialPage(final CloseableHttpAsyncClient httpclient, String uri) {
		final FutureCallback<String> k = new FutureCallback<>();
		httpclient.execute(new HttpGet(uri), new org.apache.http.concurrent.FutureCallback<HttpResponse>() {
			@Override
			public void completed(HttpResponse response) {
				try {
					String s = responseToText(response);
					k.completed(s);
				} catch (Throwable t) {	k.error(t);	}
			}

			@Override
			public void cancelled() {
				k.completed(null);
			}

			@Override
			public void failed(Exception ex) {
				k.error(ex);
			}
		});

		String page = k.getResult();
		return page;
	}


	public static String responseToText(HttpResponse response) throws IOException, ClientProtocolException {
		int status = response.getStatusLine().getStatusCode();
		if (status >= 200 && status < 300) {
			HttpEntity entity = response.getEntity();
			return entity != null ? EntityUtils.toString(entity) : null;
		} else {
			throw new ClientProtocolException("Unexpected response status: " + status);
		}
	}

}
