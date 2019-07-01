/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.backend.cyrus.replication.server.tests;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.backend.cyrus.replication.client.EmlStream;
import net.bluemind.backend.cyrus.replication.client.SyncClient;
import net.bluemind.backend.cyrus.replication.client.UnparsedResponse;
import net.bluemind.lib.vertx.VertxPlatform;

public class SyncClientToReplicationEndpointTests {

	@Before
	public void before() throws InterruptedException, ExecutionException, TimeoutException {
		System.err.println("before...");
		CompletableFuture<Void> deployed = new CompletableFuture<>();
		VertxPlatform.spawnVerticles(ar -> {
			if (ar.succeeded()) {
				deployed.complete(null);
			} else {
				deployed.completeExceptionally(ar.cause());
			}
		});
		deployed.get(10, TimeUnit.SECONDS);
		System.err.println("Deployement is complete, test starts...");
	}

	@Test
	public void testAuthenticateThenDisconnect() throws InterruptedException, ExecutionException, TimeoutException {
		SyncClient sc = new SyncClient("127.0.0.1", 2501);
		sc.connect().thenCompose(v -> {
			System.out.println("Connected");
			return sc.disconnect();
		}).thenAccept(v -> {
			System.out.println("Disconnected.");
		}).get(30, TimeUnit.SECONDS);
	}

	@Test
	public void testMailboxesWithUnbalancedParens() throws InterruptedException, ExecutionException, TimeoutException {
		SyncClient sc = new SyncClient("127.0.0.1", 2501);
		sc.connect().thenCompose(v -> {
			System.out.println("Connected");
			return sc.getMailboxes("devenv.blue!user.tom.titi (A");
		}).thenCompose(mbox -> {
			System.out.println("mbox: " + mbox);
			return sc.getMailboxes("devenv.blue!user.normal.folder", "devenv.blue!user.tom.B)");
		}).thenCompose(mbox -> {
			System.out.println("mbox: " + mbox);
			return sc.disconnect();
		}).get(30, TimeUnit.SECONDS);
	}

	@Test
	public void testFetchMissing() throws InterruptedException, ExecutionException, TimeoutException {
		SyncClient sc = new SyncClient("127.0.0.1", 2501);
		sc.connect().thenCompose(v -> {
			System.out.println("Connected");
			return sc.fetch("bm_master__devenv_blue", "devenv.blue!user.tom", "deadbeef", "aaaabbbbccccdddd", 42L);
		}).thenCompose(fetch -> {
			System.out.println("fetched " + fetch);
			return sc.disconnect();
		}).get(30, TimeUnit.SECONDS);
	}

	@Test
	public void testApplyMessages_100_10MB() throws InterruptedException, ExecutionException, TimeoutException {
		SyncClient sc = new SyncClient("127.0.0.1", 2501);
		sc.connect().thenCompose(v -> {
			System.out.println("Connected");
			EmlStream fakeStream = new EmlStream(100, 10 * 1024 * 1024, "yeah");
			CompletableFuture<UnparsedResponse> ret = sc.applyMessages(fakeStream);
			fakeStream.resume();
			return ret;
		}).thenCompose(v -> {
			System.err.println("After apply " + v);
			return sc.disconnect();
		}).thenAccept(v -> {
			System.out.println("Disconnected.");
		}).get(5, TimeUnit.MINUTES);
	}

	@Test
	public void testApplyMessages_10_100MB() throws InterruptedException, ExecutionException, TimeoutException {
		SyncClient sc = new SyncClient("127.0.0.1", 2501);
		sc.connect().thenCompose(v -> {
			System.out.println("Connected");
			EmlStream fakeStream = new EmlStream(10, 100 * 1024 * 1024, "yeah");
			CompletableFuture<UnparsedResponse> ret = sc.applyMessages(fakeStream);
			fakeStream.resume();
			return ret;
		}).thenCompose(v -> {
			System.err.println("After apply " + v);
			return sc.disconnect();
		}).thenAccept(v -> {
			System.out.println("Disconnected.");
		}).get(5, TimeUnit.MINUTES);
	}

	@After
	public void after() {
		System.err.println("test ended.");
	}

}
