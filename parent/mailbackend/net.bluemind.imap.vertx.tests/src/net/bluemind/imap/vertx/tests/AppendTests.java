/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.imap.vertx.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import org.junit.Test;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import net.bluemind.imap.vertx.ImapResponseStatus.Status;
import net.bluemind.imap.vertx.VXStoreClient;
import net.bluemind.imap.vertx.VXStoreClient.Decoder;
import net.bluemind.lib.vertx.VertxPlatform;

public class AppendTests extends WithMailboxTests {

	private static final String emlString = "From: Thomas Cataldo <thomas@zz.com>\r\n" + "Subject: test message "
			+ System.currentTimeMillis() + "\r\n" + "MIME-Version: 1.0\r\n"
			+ "Content-Type: text/plain; CHARSET=UTF-8\r\n\r\n" + "Yeah yeah.\r\n\r\n";
	private static final byte[] eml = emlString.getBytes(StandardCharsets.US_ASCII);

	@Test
	public void testSimpleEmlUploads() throws InterruptedException, ExecutionException, TimeoutException {
		VXStoreClient sc = client();
		AtomicLong uid1 = new AtomicLong();
		AtomicLong uid2 = new AtomicLong();

		sc.login().thenCompose(login -> {
			System.out.println("login finished");
			assertEquals(Status.Ok, login.status);
			FakeStream stream = new FakeStream(VertxPlatform.getVertx(), eml);
			return sc.append("INBOX", new Date(), Arrays.asList("\\Seen"), eml.length, stream);
		}).thenCompose(append -> {
			System.out.println("append 1 finished");
			assertEquals(Status.Ok, append.status);
			long uid = append.result.get().newUid;
			uid1.set(uid);
			System.out.println("Got UID1 " + uid);
			assertTrue(uid > 0);
			FakeStream stream = new FakeStream(VertxPlatform.getVertx(), eml);
			return sc.append("INBOX", new Date(), Collections.emptyList(), eml.length, stream);
		}).thenCompose(append -> {
			System.out.println("append 2 finished");
			assertEquals(Status.Ok, append.status);
			long uid = append.result.get().newUid;
			uid2.set(uid);
			System.out.println("Got UID2 " + uid);
			assertTrue(uid > 0);
			return sc.close();
		}).exceptionally(t -> {
			t.printStackTrace();
			return null;
		}).get(15, TimeUnit.SECONDS);

		System.out.println(uid1 + ", " + uid2);
		assertEquals(uid1.get() + 1, uid2.get());
	}

	@Test
	public void testAppendBIGThenFetch() throws InterruptedException, ExecutionException, TimeoutException {
		VXStoreClient sc = client();
		AtomicLong theUid = new AtomicLong();

		String bigString = "From: Thomas Cataldo <thomas@zz.com>\r\n" + "Subject: test message "
				+ System.currentTimeMillis() + "\r\n" + "MIME-Version: 1.0\r\n" + "Content-Type: fat/b64\r\n\r\n";
		ThreadLocalRandom rand = ThreadLocalRandom.current();
		byte[] m16 = new byte[16 * 1024 * 1024];
		rand.nextBytes(m16);

		bigString += Base64.getEncoder().encodeToString(m16);
		bigString += "\r\n";
		final byte[] fat = bigString.getBytes(StandardCharsets.US_ASCII);

		SlowSink sink = new SlowSink();

		sc.login().thenCompose(login -> {
			System.out.println("login finished");
			assertEquals(Status.Ok, login.status);
			FakeStream stream = new FakeStream(VertxPlatform.getVertx(), fat);
			System.err.println("Try to append " + fat.length + " byte(s)");
			return sc.append("INBOX", new Date(), Arrays.asList("\\Seen"), fat.length, stream);
		}).thenCompose(append -> {
			System.out.println("append1 finished");
			assertEquals(Status.Ok, append.status);
			long uid = append.result.get().newUid;
			assertTrue(uid > 0);
			theUid.set(uid);
			return sc.select("INBOX");
		}).thenCompose(selected -> {
			System.out.println("SELECT finished");
			assertEquals(Status.Ok, selected.status);
			return sc.fetch(theUid.get(), "1", sink, Decoder.NONE);
		}).thenCompose(fetched -> {
			System.out.println("fetch finished, got " + sink.length() + " byte(s)");
			assertTrue(sink.ended());
			ReadStream<Buffer> rs = sc.fetch(theUid.get(), "1", Decoder.NONE);
			CompletableFuture<Long> ref = new CompletableFuture<>();
			LongAdder sum = new LongAdder();
			rs.handler(b -> {
				int chunk = b.length();
				System.err.println("c1 " + chunk + " sum " + sum.sum());
				sum.add(chunk);
			});
			rs.endHandler(v -> {
				System.err.println("end c1");
				ref.complete(sum.sum());
			});
			rs.exceptionHandler(ref::completeExceptionally);
			rs.resume();
			return ref;
		}).thenCompose((Long refetched) -> {
			System.out.println("re-fetch finished, got ref: " + refetched + " sink: " + sink.length() + " byte(s)");
			assertEquals("size assert failed", refetched.longValue(), sink.length());

			ReadStream<Buffer> rs = sc.fetch(theUid.get(), "", Decoder.NONE);
			CompletableFuture<Long> ref = new CompletableFuture<>();
			LongAdder sum = new LongAdder();
			rs.handler(b -> {
				int chunk = b.length();
				System.err.println("c2 " + chunk);
				sum.add(chunk);
			});
			rs.endHandler(v -> {
				System.err.println("end c2");
				ref.complete(sum.sum());
			});
			rs.exceptionHandler(ref::completeExceptionally);
			rs.resume();
			return ref;
		}).thenCompose((Long fullFetch) -> {
			System.err.println("Full fetch is at " + fullFetch + " byte(s)");
			return sc.close();
		}).get(15, TimeUnit.SECONDS);

	}

	@Test
	public void testAppendThenFetch() throws InterruptedException, ExecutionException, TimeoutException {
		VXStoreClient sc = client();
		AtomicLong theUid = new AtomicLong();

		SlowSink sink = new SlowSink();

		sc.login().thenCompose(login -> {
			System.out.println("login finished");
			assertEquals(Status.Ok, login.status);
			FakeStream stream = new FakeStream(VertxPlatform.getVertx(), eml);
			return sc.append("INBOX", new Date(), Arrays.asList("\\Seen"), eml.length, stream);
		}).thenCompose(append -> {
			System.out.println("append1 finished");
			assertEquals(Status.Ok, append.status);
			long uid = append.result.get().newUid;
			assertTrue(uid > 0);
			theUid.set(uid);
			return sc.select("INBOX");
		}).thenCompose(selected -> {
			System.out.println("SELECT finished, now fetching...");
			assertEquals(Status.Ok, selected.status);
			return sc.fetch(theUid.get(), "1", sink, Decoder.NONE);
		}).thenCompose(fetched -> {
			System.out.println("fetch finished, got " + sink.length() + " byte(s)");
			return sc.close();
		}).get(15, TimeUnit.SECONDS);

	}

	@Test
	public void testFetchMissingUid() throws InterruptedException, ExecutionException, TimeoutException {
		VXStoreClient sc = client();

		SlowSink sink = new SlowSink();

		sc.login().thenCompose(login -> {
			System.out.println("login finished");
			assertEquals(Status.Ok, login.status);
			return sc.select("INBOX");
		}).thenCompose(selected -> {
			System.out.println("SELECT finished, now fetching...");
			assertEquals(Status.Ok, selected.status);
			return sc.fetch(123456L, "1", sink, Decoder.NONE);
		}).thenCompose(fetched -> {
			System.out.println("fetch finished, got " + sink.length() + " byte(s)");
			ReadStream<Buffer> rs = sc.fetch(123456L, "1", Decoder.NONE);
			CompletableFuture<Long> ref = new CompletableFuture<>();
			LongAdder sum = new LongAdder();
			rs.handler(b -> {
				int chunk = b.length();
				System.err.println("Chunk " + chunk);
				sum.add(chunk);
			});
			rs.endHandler(v -> {
				ref.complete(sum.sum());
			});
			rs.exceptionHandler(ref::completeExceptionally);
			rs.resume();
			return ref;
		}).thenCompose(len -> {
			System.out.println("fetch finished, got " + len + " byte(s)");
			return sc.close();
		}).get(15, TimeUnit.SECONDS);

	}

}
