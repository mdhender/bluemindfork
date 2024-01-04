/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.core.task.service;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.internal.TasksManager;
import net.bluemind.lib.vertx.VertxPlatform;

public class TaskJavaHttpTests {
	private static TasksManager taskManager;

	@BeforeClass
	public static void before() {
		VertxPlatform.spawnBlocking(60, TimeUnit.SECONDS);
		taskManager = new TasksManager(VertxPlatform.getVertx());
	}

	@After
	public void afterTest() {
		TasksManager.reset();
	}

	public TaskRef produceTask(int loops) {
		IServerTask serverTask = new BlockingServerTask() {
			@Override
			public void run(IServerTaskMonitor monitor) {
				monitor.begin(loops, "begin");
				for (int i = 0; i < loops; i++) {
					monitor.log("Coucou loop " + i);
					monitor.progress(1, null);
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}

				monitor.end(true, "yeah", "yeah");
			}
		};
		return taskManager.run(serverTask);
	}

	@Test
	public void testUsingJavaHttp() throws InterruptedException {
		int loopCount = 1000;

		System.out.println("produce task...");
		TaskRef taskRef = produceTask(loopCount);
		CountDownLatch latch = new CountDownLatch(loopCount);

		URI bluemindUri = URI.create("http://localhost:8090");
		URI taskStatusStreamUri = bluemindUri.resolve("/api/tasks/" + taskRef.id + "/_log");

		HttpClient.Builder httpClientBuilder = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
				// .executor(Executors.newVirtualThreadPerTaskExecutor())
				.connectTimeout(Duration.ofSeconds(60)).followRedirects(HttpClient.Redirect.ALWAYS);

		try (HttpClient client = httpClientBuilder.build()) {

			System.out.println("client: " + client);

			HttpRequest streamingRequest = HttpRequest.newBuilder(taskStatusStreamUri).GET()
					.header("Accept", "application/json")//
					.build();

			CompletableFuture<HttpResponse<InputStream>> response = client.sendAsync(streamingRequest,
					HttpResponse.BodyHandlers.ofInputStream());
			response.thenAccept(resp -> {
				System.out.println("received status " + resp.statusCode());
				try (InputStream in = resp.body()) {
					byte[] buf = new byte[4096];
					int len;
					while ((len = in.read(buf)) != -1) {
						System.out.println("received " + len + " " + new String(buf));
						latch.countDown();
					}
				} catch (Exception e) {
					e.printStackTrace(System.err);
				}
			});
		}
		assertTrue(latch.await(30, TimeUnit.SECONDS));
	}

	public static record TaskLog(String message, boolean end) {
	}

	@Test
	public void testUsingJavaHttpJacksonRead() throws InterruptedException {
		int loopCount = 10;

		System.out.println("produce task...");
		TaskRef taskRef = produceTask(loopCount);
		CountDownLatch latch = new CountDownLatch(loopCount);

		URI bluemindUri = URI.create("http://localhost:8090");
		URI taskStatusStreamUri = bluemindUri.resolve("/api/tasks/" + taskRef.id + "/_log");

		HttpClient.Builder httpClientBuilder = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
				.executor(Executors.newVirtualThreadPerTaskExecutor()).connectTimeout(Duration.ofSeconds(60))
				.followRedirects(HttpClient.Redirect.ALWAYS);

		try (HttpClient client = httpClientBuilder.build()) {
			HttpRequest streamingRequest = HttpRequest.newBuilder(taskStatusStreamUri).GET()
					.header("Accept", "application/json")//
					.build();

			JsonMapper mapper = JsonMapper.builder() //
					.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) //
					.disable(StreamReadFeature.AUTO_CLOSE_SOURCE) //
					.build();

			CompletableFuture<HttpResponse<InputStream>> response = client.sendAsync(streamingRequest,
					HttpResponse.BodyHandlers.ofInputStream());
			response.thenAccept(resp -> {
				try (InputStream in = resp.body(); JsonParser jsonParser = mapper.createParser(in)) {
					Optional<TaskLog> ts;
					while ((ts = readTaskLog(mapper, jsonParser)).isPresent()) {
						TaskLog tlog = ts.get();
						System.err.println(tlog);
						latch.countDown();
						if (tlog.end()) {
							break;
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}
		assertTrue(latch.await(30, TimeUnit.SECONDS));
	}

	private static Optional<TaskLog> readTaskLog(ObjectMapper mapper, JsonParser jsonParser) throws IOException {
		JsonToken tok = jsonParser.nextToken();
		if (tok == null) {
			return Optional.empty();
		}
		if (tok != JsonToken.START_OBJECT) {
			throw new IllegalStateException("We expected a json object. Got " + tok);
		}
		return Optional.of(mapper.readValue(jsonParser, TaskLog.class));
	}
}
