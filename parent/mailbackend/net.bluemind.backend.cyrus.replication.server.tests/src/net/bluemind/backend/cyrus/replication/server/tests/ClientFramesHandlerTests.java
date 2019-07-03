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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.cyrus.replication.server.tests;

import static org.junit.Assert.assertFalse;

import java.util.Collections;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetSocket;

import io.netty.buffer.Unpooled;
import net.bluemind.backend.cyrus.replication.server.ClientFramesHandler;
import net.bluemind.backend.cyrus.replication.server.ReplicationSession;
import net.bluemind.backend.cyrus.replication.server.state.StorageApiLink;
import net.bluemind.backend.cyrus.replication.storage.mock.MockStorageLinkFactory;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.vertx.testhelper.FakeNetSocket;
import net.bluemind.vertx.testhelper.WriteStreamForTests;

public class ClientFramesHandlerTests {

	/**
	 * Test requires some dirs when running in eclipse
	 * 
	 * $ sudo mkdir -p /var/spool/bm-cyrus-replication
	 * 
	 * $ sudo chown tom /var/spool/bm-cyrus-replication
	 * 
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 * 
	 */
	@Test
	public void testHandler() throws InterruptedException, ExecutionException, TimeoutException {
		Vertx vertx = VertxPlatform.getVertx();

		MockStorageLinkFactory mockFactory = new MockStorageLinkFactory();
		StorageApiLink storage = mockFactory.newLink(vertx, null, "127.0.0.1").join();
		WriteStreamForTests responseStream = new WriteStreamForTests();
		NetSocket client = new FakeNetSocket(vertx, responseStream);
		ReplicationSession session = new ReplicationSession(vertx, client, storage, Collections.emptyList());
		ClientFramesHandler underTest = new ClientFramesHandler(vertx, client, session);

		Random random = new Random(System.nanoTime());

		underTest.handle(new Buffer("APPLY CRAP ("));
		long totalSize = 0;
		long time = System.currentTimeMillis();
		for (int i = 0; i < 2048; i++) {
			int len = random.nextInt(1280000);
			byte[] content = new byte[len];
			random.nextBytes(content);

			Buffer litPrefix = new Buffer((i > 0 ? " " : "") + "%{id" + i + " {" + len + "}\r\n");
			underTest.handle(litPrefix);
			Buffer literal = new Buffer(Unpooled.wrappedBuffer(content));
			underTest.handle(literal);
			totalSize += len;
		}
		Buffer end = new Buffer(")\r\n");
		underTest.handle(end);

		responseStream.responseFuture.get(10, TimeUnit.SECONDS);
		assertFalse(responseStream.received.isEmpty());
		System.err.println("Received '" + responseStream.received.poll() + "' totalSize: " + totalSize);
		time = System.currentTimeMillis() - time;
		System.err.println("Total size: " + (totalSize) / (1024 * 1024) + "MB in " + time + "ms");

	}

}
