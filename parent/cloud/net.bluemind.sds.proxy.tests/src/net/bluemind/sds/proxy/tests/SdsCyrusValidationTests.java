package net.bluemind.sds.proxy.tests;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.Server;

public class SdsCyrusValidationTests {

	@Before
	public void before() throws InterruptedException, ExecutionException, TimeoutException {
		populateTopology();
		VertxPlatform.spawnBlocking(20, TimeUnit.SECONDS);
	}

	protected SocketAddress socket() {
		return SocketAddress.inetSocketAddress(8091, "127.0.0.1");
	}

	private RequestOptions uri(String s) {
		return new RequestOptions().setURI(s).setServer(socket());
	}

	private void populateTopology() {
		Server server = new Server();
		server.ip = "127.0.0.1";
		server.name = "localhost";
		server.tags = Lists.newArrayList("bm/core");
		ItemValue<Server> core = ItemValue.create(server.ip, server);
		List<ItemValue<Server>> servers = Lists.newArrayList(core);
		Topology.update(servers);
	}

	@Test
	public void headCallNoPayload() throws InterruptedException, ExecutionException, TimeoutException {
		HttpClient client = client();
		CompletableFuture<Integer> async = new CompletableFuture<>();
		client.request(uri("/mailbox").setMethod(HttpMethod.POST), ar -> {
			if (ar.succeeded()) {
				HttpClientRequest req = ar.result();
				req.setChunked(true);
				req.send(ar2 -> {
					if (ar2.succeeded()) {
						HttpClientResponse resp = ar2.result();
						async.complete(resp.statusCode());
					} else {
						async.completeExceptionally(ar2.cause());
					}
				});
			} else {
				async.completeExceptionally(ar.cause());
			}
		});
		int httpStatus = async.get(5, TimeUnit.SECONDS);
		assertEquals(403, httpStatus);
	}

	private HttpClient client() {
		return VertxPlatform.getVertx()
				.createHttpClient(new HttpClientOptions().setDefaultHost("127.0.0.1").setDefaultPort(8091));
	}

	@Test
	public void headCallInvalidPayload() throws InterruptedException, ExecutionException, TimeoutException {
		JsonObject payload = new JsonObject().put("invalidproperty", "anyvalue");

		HttpClient client = client();
		CompletableFuture<Integer> asyncStatusCode = new CompletableFuture<Integer>();

		client.request(uri("/mailbox").setMethod(HttpMethod.POST), ar -> {
			if (ar.succeeded()) {
				HttpClientRequest req = ar.result();
				req.setChunked(true);
				req.send(Buffer.buffer(payload.encode()), ar2 -> {
					if (ar2.succeeded()) {
						HttpClientResponse resp = ar2.result();
						System.err.println("resp " + resp);
						asyncStatusCode.complete(resp.statusCode());
					} else {
						asyncStatusCode.completeExceptionally(ar2.cause());
					}
				});
			} else {
				asyncStatusCode.completeExceptionally(ar.cause());
			}
		});

		int statusCode = asyncStatusCode.get(5, TimeUnit.SECONDS);
		assertEquals(403, statusCode);
	}

	@Test
	public void headCallMissingPartition() throws InterruptedException, ExecutionException, TimeoutException {
		JsonObject payload = new JsonObject().put("mailbox", "mailboxvalue");

		HttpClient client = client();
		CompletableFuture<Integer> asyncStatusCode = new CompletableFuture<Integer>();

		client.request(uri("/mailbox").setMethod(HttpMethod.POST), ar -> {
			if (ar.succeeded()) {
				HttpClientRequest req = ar.result();
				req.setChunked(true);
				req.send(Buffer.buffer(payload.encode()), ar2 -> {
					if (ar2.succeeded()) {
						HttpClientResponse resp = ar2.result();
						System.err.println("resp " + resp);
						asyncStatusCode.complete(resp.statusCode());
					} else {
						asyncStatusCode.completeExceptionally(ar2.cause());
					}
				});
			} else {
				asyncStatusCode.completeExceptionally(ar.cause());
			}
		});

		int statusCode = asyncStatusCode.get(5, TimeUnit.SECONDS);
		assertEquals(403, statusCode);
	}

	@Test
	public void headCallMissingMailbox() throws InterruptedException, ExecutionException, TimeoutException {
		JsonObject payload = new JsonObject().put("partition", "partitionvalue");

		HttpClient client = client();
		CompletableFuture<Integer> asyncStatusCode = new CompletableFuture<Integer>();

		client.request(uri("/mailbox").setMethod(HttpMethod.POST), ar -> {
			if (ar.succeeded()) {
				HttpClientRequest req = ar.result();
				req.setChunked(true);
				req.send(Buffer.buffer(payload.encode()), ar2 -> {
					if (ar2.succeeded()) {
						HttpClientResponse resp = ar2.result();
						System.err.println("resp " + resp);
						asyncStatusCode.complete(resp.statusCode());
					} else {
						asyncStatusCode.completeExceptionally(ar2.cause());
					}
				});
			} else {
				asyncStatusCode.completeExceptionally(ar.cause());
			}
		});

		int statusCode = asyncStatusCode.get(5, TimeUnit.SECONDS);
		assertEquals(403, statusCode);
	}

	@Test
	public void headCallValidPayload() throws InterruptedException, ExecutionException, TimeoutException {
		JsonObject payload = new JsonObject()//
				.put("mailbox", "mailboxvalue")//
				.put("partition", "partitionvalue").put("mboxpath", "/tmp/mbox");

		HttpClient client = client();
		CompletableFuture<Integer> asyncStatusCode = new CompletableFuture<Integer>();

		client.request(uri("/mailbox").setMethod(HttpMethod.POST), ar -> {
			if (ar.succeeded()) {
				HttpClientRequest req = ar.result();
				req.setChunked(true);
				req.send(Buffer.buffer(payload.encode()), ar2 -> {
					if (ar2.succeeded()) {
						HttpClientResponse resp = ar2.result();
						System.err.println("resp " + resp);
						asyncStatusCode.complete(resp.statusCode());
					} else {
						asyncStatusCode.completeExceptionally(ar2.cause());
					}
				});
			} else {
				asyncStatusCode.completeExceptionally(ar.cause());
			}
		});

		int statusCode = asyncStatusCode.get(5, TimeUnit.SECONDS);
		assertEquals(200, statusCode);
	}
}
