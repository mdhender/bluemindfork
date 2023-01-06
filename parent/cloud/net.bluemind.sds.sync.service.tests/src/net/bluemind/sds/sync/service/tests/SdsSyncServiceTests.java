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
package net.bluemind.sds.sync.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.hash.Hashing;

import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.sds.sync.api.ISdsSync;
import net.bluemind.sds.sync.api.SdsSyncEvent;
import net.bluemind.sds.sync.api.SdsSyncEvent.Body;
import net.bluemind.sds.sync.service.internal.queue.SdsSyncQueue;
import net.openhft.chronicle.queue.TailerDirection;

public class SdsSyncServiceTests {
	public static Path sdsSyncQueuePath;
	private SdsSyncQueue queue;

	@BeforeClass
	public static void beforeQueueTests() throws IOException {
		sdsSyncQueuePath = Files.createTempDirectory("sdsqueue");
		System.setProperty("bm.sdssyncqueue", sdsSyncQueuePath.toString());
	}

	@After
	public void afterQueueTests() throws Exception {
		Files.walk(sdsSyncQueuePath).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		queue.close();
	}

	@Before
	public void before() {
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
	}

	@Before
	public void populateQueue() throws Exception {
		queue = new SdsSyncQueue();
		byte[] x = { (byte) 0x01, (byte) 0x02 };
		queue.putBody(SdsSyncEvent.BODYADD, new Body(x, "bm-master"));
		queue.putBody(SdsSyncEvent.BODYDEL, new Body(x, "bm-master"));
		queue.putBody(SdsSyncEvent.BODYADD, new Body(x, "bm-master"));
		queue.putFileHosting(SdsSyncEvent.FHADD, "bonjour/toi");
	}

	@Test(expected = ServerFault.class)
	public void noAccess() throws InterruptedException {
		var sdsSyncApi = ServerSideServiceProvider.getProvider(SecurityContext.ANONYMOUS).instance(ISdsSync.class);
		ReadStream<JsonObject> reader = VertxStream.read(sdsSyncApi.sync(-1L));
	}

	@Test
	public void streamReadall() throws InterruptedException {
		var sdsSyncApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ISdsSync.class);
		ReadStream<JsonObject> reader = VertxStream.read(sdsSyncApi.sync(-1L));
		var cdl = new CountDownLatch(4);
		reader.endHandler(v -> System.err.println("ended"));
		reader.handler(body -> {
			System.err.println("body: " + body.encodePrettily());
			cdl.countDown();
		});
		reader.exceptionHandler(t -> System.err.println("err: " + t));
		reader.resume();

		assertTrue(cdl.await(5, TimeUnit.SECONDS));
	}

	@Test
	public void streamReadFromindex() throws InterruptedException {
		var sdsSyncApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ISdsSync.class);
		var lastIndex = queue.createTailer().toEnd().direction(TailerDirection.BACKWARD).index() - 1;
		ReadStream<JsonObject> reader = VertxStream.read(sdsSyncApi.sync(lastIndex));
		var cdl = new CountDownLatch(1);
		reader.endHandler(v -> System.err.println("ended"));
		reader.handler(body -> {
			System.err.println("body: " + body.encodePrettily());
			assertEquals(Long.valueOf(lastIndex), body.getLong("index"));
			cdl.countDown();
		});
		reader.exceptionHandler(t -> System.err.println("err: " + t));
		reader.resume();

		assertTrue(cdl.await(5, TimeUnit.SECONDS));
	}

	@Test
	public void fullPush() throws Exception {
		queue = new SdsSyncQueue();
		long start = System.nanoTime();
		for (var i = 0; i < 1_000_000; i++) {
			@SuppressWarnings("deprecation")
			var randomHash = Hashing.sha1().hashString(UUID.randomUUID().toString(), StandardCharsets.US_ASCII)
					.asBytes();
			queue.putBody(SdsSyncEvent.BODYADD, new Body(randomHash, "bm-master"));
		}
		System.err.println("pushed 1 000 000 items in " + ((System.nanoTime() - start) / 1000000) + " ms");
		Files.walk(sdsSyncQueuePath).forEach(p -> {
			try {
				// Expected ~ 62 bytes/message
				System.err.println(p.toString() + " " + String.valueOf(Files.size(p)));
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
}
