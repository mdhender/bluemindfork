package net.bluemind.metrics.core.tick.client;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;

import io.vertx.core.json.JsonObject;

public class JsonHttpHelper implements AutoCloseable {

	private final AsyncHttpClient client;

	public JsonHttpHelper() {
		this.client = new DefaultAsyncHttpClient();
	}

	public JsonObject get(String url) throws InterruptedException, ExecutionException, TimeoutException, IOException {
		Response response = client.prepareGet(url).execute().get(10, TimeUnit.SECONDS);
		return new JsonObject(response.getResponseBody());
	}

	public void sendPost(String url, JsonObject content) throws Exception {
		client.preparePost(url).setBody(content.encode().getBytes()).execute().get(10, TimeUnit.SECONDS);
	}

	public void sendPut(String url, JsonObject content) throws Exception {
		client.preparePut(url).setBody(content.encode().getBytes()).execute().get(10, TimeUnit.SECONDS);
	}

	public void close() {
		try {
			client.close();
		} catch (IOException e) {
			// ok
		}
	}

}
