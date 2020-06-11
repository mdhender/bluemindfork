/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.backend.cyrus.bmgroups;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.channels.Channels;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;

import io.vertx.core.buffer.Buffer;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;
import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.domain.api.Domain;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.network.topology.Topology;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class GroupProtocolTests {

	@BeforeClass
	public static void init() throws IOException {
		GroupProtocolVerticle.socketPath(File.createTempFile("socket", "pt").getAbsolutePath());
	}

	private ItemValue<Domain> domain;
	private String userUid;

	@Before
	public void setup() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		ElasticsearchTestHelper.getInstance().beforeTest();

		String domainUid = "dom" + System.currentTimeMillis() + ".test";

		String cyrusIp = new BmConfIni().get("imap-role");
		Server imapServer = new Server();
		imapServer.ip = cyrusIp;
		imapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(imapServer);

		VertxPlatform.spawnBlocking(20, TimeUnit.SECONDS);
		Topology.get();

		domain = PopulateHelper.createTestDomain(domainUid, imapServer);

		// create domain parititon on cyrus
		new CyrusService(cyrusIp).createPartition(domainUid);
		new CyrusService(cyrusIp).refreshPartitions(Arrays.asList(domainUid));
		new CyrusService(cyrusIp).reload();

		userUid = PopulateHelper.addUser("check", domain.uid, Routing.internal);
		System.err.println("check@" + domain.uid);
	}

	@Test
	public void testRequestUser() throws InterruptedException, IOException {

		long time = System.currentTimeMillis();
		UnixSocketAddress address = new UnixSocketAddress(new File(GroupProtocolVerticle.socketPath()));
		try (UnixSocketChannel channel = UnixSocketChannel.open(address)) {
			assertTrue(channel.isConnected());
			System.err.println("time to connect " + (System.currentTimeMillis() - time));
			PrintWriter w = new PrintWriter(Channels.newOutputStream(channel));
			w.print("check@" + domain.uid);
			w.flush();
			System.err.println("time to send question " + (System.currentTimeMillis() - time));
			assertTrue(channel.isConnected());
			try (InputStream in = Channels.newInputStream(channel)) {
				byte[] read = ByteStreams.toByteArray(in);
				assertNotNull(read);
				assertTrue(read.length >= 2);
				Buffer data = Buffer.buffer(read);
				System.err.println("read data " + data);
				System.err.println("time to response " + (System.currentTimeMillis() - time));
				short length = data.getShort(0);
				String v = data.getString(2, 2 + length);
				assertTrue(v.startsWith("OK"));
				String[] id = v.substring(2, v.length()).split(",");
				System.out.println("id " + Arrays.asList(id));
				assertTrue(Arrays.asList(id).contains(userUid + "@" + domain.uid));
			}

			w.close();
		}
	}

	@Test
	public void testLongRun() throws InterruptedException, IOException {
		for (int i = 0; i < 10000; i++) {
			try {
				testRequestUser();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	public void testParallel() throws InterruptedException, IOException {
		ExecutorService ee = Executors.newFixedThreadPool(10);

		LinkedList<Future<?>> futures = new LinkedList<>();
		for (int i = 0; i < 10000; i++) {
			Future<?> futre = ee.submit(new Runnable() {

				@Override
				public void run() {

					try {
						testRequestUser();
					} catch (Exception e) {
						e.printStackTrace();
						throw new RuntimeException(e);
					}
				}

			});
			futures.add(futre);
		}

		LinkedList<Exception> errors = new LinkedList<>();
		for (Future<?> f : futures) {
			try {
				f.get();
			} catch (Exception e) {
				errors.add(e);
			}
		}

		if (!errors.isEmpty()) {
			for (Exception e : errors) {
				if (e.getCause() != null) {
					e.printStackTrace();
				} else {
					System.err.println("error " + e.getMessage());
				}
			}
		}
		assertTrue(errors.isEmpty());
	}

}
