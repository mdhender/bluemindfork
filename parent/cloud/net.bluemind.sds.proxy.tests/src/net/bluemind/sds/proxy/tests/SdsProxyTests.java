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
		CompletableFuture<Void> startResult = new CompletableFuture<>();
		VertxPlatform.spawnVerticles(spawnResult -> {
			if (spawnResult.succeeded()) {
				startResult.complete(null);
			} else {
				startResult.completeExceptionally(spawnResult.cause());
			}
		});
		startResult.get(20, TimeUnit.SECONDS);

		this.root = new File(System.getProperty("user.home"), "dummy-sds");
		root.mkdirs();
		Files.createFile(new File(root, "123").toPath());
		Files.createFile(new File(root, "orig.txt").toPath());
	}

	private HttpClient client() {
		return VertxPlatform.getVertx().createHttpClient(new HttpClientOptions());
	}

	protected SocketAddress socket() {
		return SocketAddress.inetSocketAddress(8091, "127.0.0.1");
	}

	private RequestOptions uri(String s) {
		return new RequestOptions().setURI(s);
	}

	@After
	public void after() {
		Arrays.stream(root.listFiles(file -> file.isFile())).forEach(File::delete);
		root.delete();

		CompletableFuture<Integer> waitResp = new CompletableFuture<>();
		HttpClient client = client();
		JsonObject payload = new JsonObject().put("storeType", "dummy");
		client.request(HttpMethod.POST, socket(), uri("/configuration"), resp -> {
			System.err.println("resp " + resp);
			resp.exceptionHandler(t -> waitResp.completeExceptionally(t));
			resp.endHandler(v -> {
				System.err.println(resp.statusCode());
				waitResp.complete(resp.statusCode());
			});
		}).setChunked(true).write(Buffer.buffer(payload.encode())).end();
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
		client.request(HttpMethod.OPTIONS, socket(), uri("/sds"), resp -> {
			System.err.println("resp " + resp);
			resp.exceptionHandler(t -> waitResp.completeExceptionally(t));
			resp.endHandler(v -> {
				System.err.println(resp.statusCode());
				waitResp.complete(resp.statusCode());
			});
		}).setChunked(true).write(Buffer.buffer(payload.encode())).end();
		System.err.println("started");
		int httpStatus = waitResp.get(5, TimeUnit.SECONDS);
		assertEquals(200, httpStatus);
	}

	@Test
	public void testHeadMissingCall() throws InterruptedException, ExecutionException, TimeoutException {
		CompletableFuture<Integer> waitResp = new CompletableFuture<>();
		HttpClient client = client();
		JsonObject payload = new JsonObject().put("mailbox", "yeah").put("guid", "789");
		client.request(HttpMethod.OPTIONS, socket(), uri("/sds"), resp -> {
			System.err.println("resp " + resp);
			resp.exceptionHandler(t -> waitResp.completeExceptionally(t));
			resp.endHandler(v -> {
				System.err.println(resp.statusCode());
				waitResp.complete(resp.statusCode());
			});
		}).setChunked(true).write(Buffer.buffer(payload.encode())).end();
		System.err.println("started");
		int httpStatus = waitResp.get(5, TimeUnit.SECONDS);
		assertEquals(404, httpStatus);
	}

	@Test
	public void testDeleteCall() throws InterruptedException, ExecutionException, TimeoutException {
		CompletableFuture<Integer> waitResp = new CompletableFuture<>();
		HttpClient client = client();
		JsonObject payload = new JsonObject().put("mailbox", "yeah").put("guid", "123");
		client.request(HttpMethod.DELETE, socket(), uri("/sds"), resp -> {
			System.err.println("resp " + resp);
			resp.exceptionHandler(t -> waitResp.completeExceptionally(t));
			resp.endHandler(v -> {
				System.err.println(resp.statusCode());
				waitResp.complete(resp.statusCode());
			});
		}).setChunked(true).write(Buffer.buffer(payload.encode())).end();
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
		client.request(HttpMethod.PUT, socket(), uri("/sds"), resp -> {
			System.err.println("resp " + resp);
			resp.exceptionHandler(t -> waitResp.completeExceptionally(t));
			resp.endHandler(v -> {
				System.err.println(resp.statusCode());
				waitResp.complete(resp.statusCode());
			});
		}).setChunked(true).write(Buffer.buffer(payload.encode())).end();
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
		client.request(HttpMethod.GET, socket(), uri("/sds"), resp -> {
			resp.exceptionHandler(t -> waitResp.completeExceptionally(t));
			resp.endHandler(v -> {
				System.err.println(resp.statusCode());
				waitResp.complete(resp.statusCode());
			});
		}).setChunked(true).write(Buffer.buffer(payload.encode())).end();
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
		client.request(HttpMethod.POST, socket(), uri("/sds/mget"), resp -> {
			resp.exceptionHandler(t -> waitResp.completeExceptionally(t));
			resp.endHandler(v -> {
				System.err.println(resp.statusCode());
				waitResp.complete(resp.statusCode());
			});
		}).setChunked(true).write(Buffer.buffer(payload.encode())).end();
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

		JsonObject payload = new JsonObject().put("storeType", "test");
		client.request(HttpMethod.POST, socket(), uri("/configuration"), resp -> {
			System.err.println("resp " + resp);
			resp.exceptionHandler(t -> waitResp.completeExceptionally(t));
			resp.endHandler(v -> {
				System.err.println(resp.statusCode());
				waitResp.complete(resp.statusCode());
			});
		}).setChunked(true).write(Buffer.buffer(payload.encode())).end();

		System.err.println("started");
		int httpStatus = waitResp.get(5, TimeUnit.SECONDS);
		assertEquals(200, httpStatus);

		payload = new JsonObject().put("mailbox", "yeah").put("guid", "123");
		client.request(HttpMethod.OPTIONS, socket(), uri("/sds"), resp -> {
			resp.endHandler(v -> {
			});
		}).setChunked(true).write(Buffer.buffer(payload.encode())).end();

		assertTrue(reconfigured.get(5, TimeUnit.SECONDS));
		assertNotNull(storeMsg.get(5, TimeUnit.SECONDS));
		assertNotNull(existCall.get(5, TimeUnit.SECONDS));
	}

}
