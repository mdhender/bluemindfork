package net.bluemind.metrics.core.tick.client;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.vertx.java.core.json.JsonObject;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

public class JsonHttpHelper implements AutoCloseable {

	private final AsyncHttpClient client;

	public JsonHttpHelper() {
		this.client = new AsyncHttpClient();
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
		client.close();
	}

}
