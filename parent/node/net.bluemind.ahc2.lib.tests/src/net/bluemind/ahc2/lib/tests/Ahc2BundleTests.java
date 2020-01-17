package net.bluemind.ahc2.lib.tests;

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.TimeUnit;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import org.junit.Test;

import io.netty.handler.ssl.SslContextBuilder;

public class Ahc2BundleTests {

	@Test
	public void testHttpsGetOnGoogle() throws Exception {
		DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder();
		builder.setUseInsecureTrustManager(true);
		builder.setSslContext(SslContextBuilder.forClient().build());
		builder.setUseNativeTransport(true);
		AsyncHttpClientConfig conf = builder.build();
		AsyncHttpClient ahc = new DefaultAsyncHttpClient(conf);
		System.err.println("Got ahc " + ahc);
		BoundRequestBuilder req = ahc.prepareGet("https://www.google.com/");
		System.err.println("req: " + req);
		ListenableFuture<Response> future = req.execute();
		assertNotNull(future.get(10, TimeUnit.SECONDS));

		ahc.close();

	}

}
