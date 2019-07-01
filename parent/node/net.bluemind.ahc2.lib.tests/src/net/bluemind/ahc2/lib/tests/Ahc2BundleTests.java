package net.bluemind.ahc2.lib.tests;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.junit.Test;

public class Ahc2BundleTests {

	@Test
	public void testHttpsGetOnGoogle() throws Exception {
		DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder();
		builder.setAcceptAnyCertificate(true);
		AsyncHttpClientConfig conf = builder.build();
		AsyncHttpClient ahc = new DefaultAsyncHttpClient(conf);

		CompletableFuture<Integer> promise = ahc.prepareGet("https://www.google.com/").execute().toCompletableFuture()//
				.thenApply(resp -> resp.getStatusCode());
		assertEquals(new Integer(200), promise.get(1, TimeUnit.MINUTES));
		ahc.close();

	}

}
