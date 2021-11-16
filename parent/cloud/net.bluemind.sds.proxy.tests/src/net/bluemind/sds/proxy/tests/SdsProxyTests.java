/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.sds.proxy.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import net.bluemind.lib.vertx.VertxPlatform;

public class SdsProxyTests {

	private File root;

	@Before
	public void before() throws InterruptedException, ExecutionException, TimeoutException, IOException {
		VertxPlatform.spawnBlocking(20, TimeUnit.SECONDS);

		this.root = new File(System.getProperty("user.home"), "dummy-sds");
		if (!root.mkdirs()) {
			System.err.println("Unable to mkdirs() on " + root);
		}
		try {
			Files.createFile(new File(root, "123").toPath());
		} catch (FileAlreadyExistsException e) {
		}
		try {
			Files.createFile(new File(root, "orig.txt").toPath());
		} catch (FileAlreadyExistsException e) {
		}
	}

	private HttpClient client() {
		return VertxPlatform.getVertx().createHttpClient(new HttpClientOptions());
	}

	protected SocketAddress socket() {
		return SocketAddress.inetSocketAddress(8091, "127.0.0.1");
	}

	private RequestOptions uri(String s) {
		return new RequestOptions().setURI(s).setServer(socket());
	}

	@After
	public void after() {
		Arrays.stream(root.listFiles(file -> file.isFile())).forEach(File::delete);
		root.delete();

		CompletableFuture<Integer> waitResp = new CompletableFuture<>();
		HttpClient client = client();
		JsonObject payload = new JsonObject().put("storeType", "dummy");

		client.request(uri("/configuration").setMethod(HttpMethod.POST), ar -> {
			if (ar.succeeded()) {
				HttpClientRequest req = ar.result();
				req.setChunked(true);
				req.send(Buffer.buffer(payload.encode()), ar2 -> {
					if (ar2.succeeded()) {
						HttpClientResponse resp = ar2.result();
						System.err.println("resp " + resp);
						waitResp.complete(resp.statusCode());
					} else {
						waitResp.completeExceptionally(ar2.cause());
					}
				});
			} else {
				waitResp.completeExceptionally(ar.cause());
			}
		});
		try {
			waitResp.get(30, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testHeadCall() throws InterruptedException, ExecutionException, TimeoutException {
		HttpClient client = client();
		JsonObject payload = new JsonObject().put("mailbox", "yeah").put("guid", "123");
		CompletableFuture<Integer> waitResp = new CompletableFuture<>();
		client.request(uri("/sds").setMethod(HttpMethod.OPTIONS), ar -> {
			if (ar.succeeded()) {
				HttpClientRequest req = ar.result();
				req.setChunked(true);
				req.send(Buffer.buffer(payload.encode()), ar2 -> {
					if (ar2.succeeded()) {
						HttpClientResponse resp = ar2.result();
						System.err.println("resp " + resp);
						waitResp.complete(resp.statusCode());
					} else {
						waitResp.completeExceptionally(ar2.cause());
					}
				});
			} else {
				waitResp.completeExceptionally(ar.cause());
			}
		});
		System.err.println("started");
		int httpStatus = waitResp.get(5, TimeUnit.SECONDS);
		assertEquals(200, httpStatus);
	}

	@Test
	public void testHeadMissingCall() throws InterruptedException, ExecutionException, TimeoutException {
		CompletableFuture<Integer> waitResp = new CompletableFuture<>();
		HttpClient client = client();
		JsonObject payload = new JsonObject().put("mailbox", "yeah").put("guid", "789");
		client.request(uri("/sds").setMethod(HttpMethod.OPTIONS)).onSuccess(req -> {
			req.setChunked(true);
			req.write(Buffer.buffer(payload.encode()));
			req.response().onSuccess(resp -> {
				System.err.println("resp " + resp);
				waitResp.complete(resp.statusCode());
			}).onFailure(t -> waitResp.completeExceptionally(t));
		});
		System.err.println("started");
		int httpStatus = waitResp.get(5, TimeUnit.SECONDS);
		assertEquals(404, httpStatus);
	}

	@Test
	public void testDeleteCall() throws InterruptedException, ExecutionException, TimeoutException {
		CompletableFuture<Integer> waitResp = new CompletableFuture<>();
		HttpClient client = client();
		JsonObject payload = new JsonObject().put("mailbox", "yeah").put("guid", "123");
		client.request(uri("/sds").setMethod(HttpMethod.DELETE), ar -> {
			if (ar.succeeded()) {
				HttpClientRequest req = ar.result();
				req.setChunked(true);
				req.send(Buffer.buffer(payload.encode()), ar2 -> {
					if (ar2.succeeded()) {
						HttpClientResponse resp = ar2.result();
						System.err.println("resp " + resp);
						waitResp.complete(resp.statusCode());
					} else {
						waitResp.completeExceptionally(ar2.cause());
					}
				});
			} else {
				waitResp.completeExceptionally(ar.cause());
			}
		});
		System.err.println("started");
		int httpStatus = waitResp.get(5, TimeUnit.SECONDS);
		assertEquals(200, httpStatus);
	}

	@Test
	public void testPutCall() throws InterruptedException, ExecutionException, TimeoutException {
		CompletableFuture<Integer> waitResp = new CompletableFuture<>();
		HttpClient client = client();
		JsonObject payload = new JsonObject().put("mailbox", "yeah").put("guid", "put.dest").put("filename",
				new File(root, "orig.txt").getAbsolutePath());
		client.request(uri("/sds").setMethod(HttpMethod.PUT), ar -> {
			if (ar.succeeded()) {
				HttpClientRequest req = ar.result();
				req.setChunked(true);
				req.send(Buffer.buffer(payload.encode()), ar2 -> {
					if (ar2.succeeded()) {
						HttpClientResponse resp = ar2.result();
						System.err.println("resp " + resp);
						waitResp.complete(resp.statusCode());
					} else {
						waitResp.completeExceptionally(ar2.cause());
					}
				});
			} else {
				waitResp.completeExceptionally(ar.cause());
			}
		});
		System.err.println("started");
		int httpStatus = waitResp.get(5, TimeUnit.SECONDS);
		assertEquals(200, httpStatus);
		assertTrue(new File(root, "put.dest").exists());
	}

	@Test
	public void testGetCall() throws InterruptedException, ExecutionException, TimeoutException {
		CompletableFuture<Integer> waitResp = new CompletableFuture<>();
		HttpClient client = client();
		JsonObject payload = new JsonObject().put("mailbox", "yeah").put("guid", "123").put("filename",
				new File(root, "dest.txt").getAbsolutePath());
		client.request(uri("/sds").setMethod(HttpMethod.GET)).onSuccess(req -> {
			req.setChunked(true);
			req.send(Buffer.buffer(payload.encode())) //
					.onSuccess(resp -> {
						waitResp.complete(resp.statusCode());
					}) //
					.onFailure(t -> {
						waitResp.completeExceptionally(t);
					});
		}).onFailure(t -> System.err.println("request failed" + t));
		System.err.println("started");
		int httpStatus = waitResp.get(5, TimeUnit.SECONDS);
		assertEquals(200, httpStatus);
		assertTrue("dest was not created", new File(root, "dest.txt").exists());
	}

	@Test
	public void testMgetCall() throws InterruptedException, ExecutionException, TimeoutException {
		CompletableFuture<Integer> waitResp = new CompletableFuture<>();
		HttpClient client = client();
		JsonObject payload = new JsonObject().put("mailbox", "yeah");
		JsonArray transfers = new JsonArray();
		for (int i = 0; i < 500; i++) {
			transfers.add(new JsonObject().put("guid", "123").put("filename",
					new File(root, "dest" + i + ".txt").getAbsolutePath()));
		}
		payload.put("transfers", transfers);
		client.request(uri("/sds/mget").setMethod(HttpMethod.POST), ar -> {
			if (ar.succeeded()) {
				HttpClientRequest req = ar.result();
				req.setChunked(true);
				req.send(Buffer.buffer(payload.encode()), ar2 -> {
					if (ar2.succeeded()) {
						HttpClientResponse resp = ar2.result();
						System.err.println("resp " + resp);
						waitResp.complete(resp.statusCode());
					} else {
						waitResp.completeExceptionally(ar2.cause());
					}
				});
			} else {
				waitResp.completeExceptionally(ar.cause());
			}
		});
		System.err.println("started");
		int httpStatus = waitResp.get(5, TimeUnit.SECONDS);
		assertEquals(200, httpStatus);
		assertTrue("dest1 was not created", new File(root, "dest1.txt").exists());
		assertTrue("dest2 was not created", new File(root, "dest2.txt").exists());
	}

	@Test
	public void testConfigureCall() throws InterruptedException, ExecutionException, TimeoutException {
		CompletableFuture<Integer> waitResp = new CompletableFuture<>();
		HttpClient client = client();
		CompletableFuture<Boolean> reconfigured = new CompletableFuture<>();
		VertxPlatform.eventBus().consumer("sds.events.configuration.updated", (Message<Boolean> msg) -> {
			System.err.println("reconfigured.");
			reconfigured.complete(msg.body());
		});

		CompletableFuture<JsonObject> storeMsg = new CompletableFuture<>();
		VertxPlatform.eventBus().consumer("test.store.configured", (Message<JsonObject> msg) -> {
			System.err.println("reconfigured with " + msg.body());
			storeMsg.complete(msg.body());
		});

		CompletableFuture<String> existCall = new CompletableFuture<>();
		VertxPlatform.eventBus().consumer("test.store.exists", (Message<String> msg) -> {
			System.err.println("exist test with " + msg.body());
			existCall.complete(msg.body());
		});

		client.request(uri("/configuration").setMethod(HttpMethod.POST), ar -> {
			if (ar.succeeded()) {
				HttpClientRequest req = ar.result();
				req.setChunked(true);
				JsonObject payload = new JsonObject().put("storeType", "test");
				req.send(Buffer.buffer(payload.encode()), ar2 -> {
					if (ar2.succeeded()) {
						HttpClientResponse resp = ar2.result();
						System.err.println("resp " + resp);
						waitResp.complete(resp.statusCode());
					} else {
						waitResp.completeExceptionally(ar2.cause());
					}
				});
			} else {
				waitResp.completeExceptionally(ar.cause());
			}
		});

		System.err.println("started");
		int httpStatus = waitResp.get(5, TimeUnit.SECONDS);
		assertEquals(200, httpStatus);

		JsonObject payload = new JsonObject().put("mailbox", "yeah").put("guid", "123");
		client.request(uri("/sds").setMethod(HttpMethod.OPTIONS)).onSuccess(req -> {
			req.setChunked(true);
			req.send(Buffer.buffer(payload.encode()));
		});
		assertTrue(reconfigured.get(5, TimeUnit.SECONDS));
		assertNotNull(storeMsg.get(5, TimeUnit.SECONDS));
		assertNotNull(existCall.get(5, TimeUnit.SECONDS));
	}

}
