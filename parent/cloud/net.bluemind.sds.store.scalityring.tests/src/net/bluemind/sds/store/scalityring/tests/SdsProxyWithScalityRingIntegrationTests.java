/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.sds.store.scalityring.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.vertx.core.json.JsonObject;
import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.cyrus.replication.testhelper.CyrusReplicationHelper;
import net.bluemind.backend.cyrus.replication.testhelper.SyncServerHelper;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.dockerclient.DockerEnv;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.StoreClient;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.locator.LocatorVerticle;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.network.topology.Topology;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.pool.impl.docker.DockerContainer;
import net.bluemind.sds.dto.GetRequest;
import net.bluemind.sds.dto.MgetRequest;
import net.bluemind.sds.dto.MgetRequest.Transfer;
import net.bluemind.sds.proxy.mgmt.SdsProxyManager;
import net.bluemind.sds.proxy.testhelper.ObjectStoreTestHelper;
import net.bluemind.sds.store.ISdsBackingStore;
import net.bluemind.sds.store.scalityring.ScalityConfiguration;
import net.bluemind.sds.store.scalityring.ScalityRingStoreFactory;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.vertx.testhelper.Deploy;

public class SdsProxyWithScalityRingIntegrationTests {

	private CyrusReplicationHelper cyrusReplication;
	private String domainUid;
	private String userUid;
	private String cyrusIp;
	private ScalityConfiguration config;
	private CyrusPartition partition;

	private ScalityConfiguration getScalityConfig() {
		String scalityip = DockerEnv.getIp(DockerContainer.SCALITYRING.getName());
		return new ScalityConfiguration("http://" + scalityip + ":81/proxy/local");
	}

	private ISdsBackingStore getStore() {
		JsonObject configjs = getScalityConfig().asJson();
		return new ScalityRingStoreFactory().create(VertxPlatform.getVertx(), configjs);
	}

	@Before
	public void before() throws Exception {
		try {
			JdbcTestHelper.getInstance().beforeTest();
			Deploy.verticles(false, LocatorVerticle::new).get(5, TimeUnit.SECONDS);

			BmConfIni ini = new BmConfIni();

			Server esServer = new Server();
			esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
			System.out.println("ES is " + esServer.ip);
			assertNotNull(esServer.ip);
			esServer.tags = Lists.newArrayList("bm/es");

			this.cyrusIp = ini.get("imap-role");
			Server imapServer = new Server();
			imapServer.ip = cyrusIp;
			imapServer.tags = Lists.newArrayList("mail/imap");

			ItemValue<Server> cyrusServer = ItemValue.create("imapserver", imapServer);
			CyrusService cyrusService = new CyrusService(cyrusServer);
			cyrusService.reset();

			Server core = new Server();
			core.ip = getMyIpAddress();
			core.tags = Lists.newArrayList("bm/core", "bm/pgsql", "bm/pgsql-data");

			PopulateHelper.initGlobalVirt(false, core, esServer, imapServer);
			ElasticsearchTestHelper.getInstance().beforeTest();
			PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.none);

			String unique = "" + System.currentTimeMillis();
			domainUid = "test" + unique + ".lab";
			userUid = "user" + unique;

			ObjectStoreTestHelper.setupscality(cyrusService);
			Thread.sleep(2000); // maybe ?

			// ensure the partition is created correctly before restarting cyrus
			PopulateHelper.addDomain(domainUid, Routing.none);

			System.err.println("Setup replication START");
			this.cyrusReplication = new CyrusReplicationHelper(cyrusIp);
			cyrusReplication.installReplication();
			System.err.println("Setup replication END");

			JdbcActivator.getInstance().addMailboxDataSource(cyrusReplication.server().uid,
					JdbcTestHelper.getInstance().getMailboxDataDataSource());

			CountDownLatch cdl = new CountDownLatch(1);
			VertxPlatform.spawnVerticles(ar -> cdl.countDown());
			boolean beforeTimeout = cdl.await(30, TimeUnit.SECONDS);
			assertTrue(beforeTimeout);

			MQ.init().get(30, TimeUnit.SECONDS);

			Topology.get();

			this.partition = CyrusPartition.forServerAndDomain(Topology.get().any("mail/imap"), domainUid);

			SyncServerHelper.waitFor();

			ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
			ISystemConfiguration sysConfApi = prov.instance(ISystemConfiguration.class);
			ImmutableMap<String, String> dummyConf = new ImmutableMap.Builder<String, String>() //
					.put(SysConfKeys.archive_kind.name(), "dummy") //
					.put(SysConfKeys.archive_days.name(), "0") //
					.build();
			sysConfApi.updateMutableValues(dummyConf);

			cyrusReplication.startReplication().get(5, TimeUnit.SECONDS);
			this.config = getScalityConfig();
			ImmutableMap<String, String> freshConf = new ImmutableMap.Builder<String, String>() //
					.put(SysConfKeys.archive_kind.name(), "scalityring") //
					.put(SysConfKeys.sds_s3_endpoint.name(), config.getEndpoint()) //
					.put(SysConfKeys.archive_days.name(), "0") //
					.build();
			sysConfApi.updateMutableValues(freshConf);

			System.err.println("Start populate user " + userUid);
			PopulateHelper.addUser(userUid, domainUid, Routing.internal);
			System.err.println("Populated.");
			// Wait for sds proxy to join topology...
			Thread.sleep(2_000);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	private static String getMyIpAddress() {
		String ret = "127.0.0.1";
		try {
			Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
			while (ifaces.hasMoreElements()) {
				NetworkInterface iface = ifaces.nextElement();
				if (iface.isLoopback() || !iface.isUp()) {
					continue;
				}
				List<InterfaceAddress> addresses = iface.getInterfaceAddresses();
				for (InterfaceAddress ia : addresses) {
					if (ia.getBroadcast() == null) {
						// ipv6
						continue;
					}
					String tmp = ia.getAddress().getHostAddress();
					if (!tmp.startsWith("127")) {
						return tmp;
					}
				}
			}
		} catch (SocketException e) {
		}
		return ret;
	}

	@After
	public void after() throws Exception {
		System.err.println("Waiting for last events...");
		Thread.sleep(1000);
		cyrusReplication.stopReplication().get(5, TimeUnit.SECONDS);
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void configureSdsProxy() throws InterruptedException, ExecutionException, TimeoutException, IOException {
		// configure sds-proxy for scality
		try (SdsProxyManager sdsMgmt = new SdsProxyManager(VertxPlatform.getVertx(), cyrusIp)) {
			sdsMgmt.applyConfiguration(config.asJson()).get(5, TimeUnit.SECONDS);
		}

		// append mail
		String eml = "From: patrick" + System.currentTimeMillis() + "@junit.test\r\n\r\n";
		for (int i = 0; i < 50 * 1014; i++) {
			eml += "aa";
		}
		byte[] emlData = eml.getBytes();
		@SuppressWarnings("deprecation")
		ByteBuf hash = Unpooled.wrappedBuffer(Hashing.sha1().hashBytes(emlData).asBytes());
		String guid = ByteBufUtil.hexDump(hash);
		System.err.println("Body guid should be " + guid);
		try (StoreClient sc = new StoreClient(cyrusIp, 1143, userUid + "@" + domainUid, userUid)) {
			sc.login();
			int added = sc.append("INBOX", new ByteArrayInputStream(emlData), new FlagsList());
			assertTrue(added > 0);
		}

		// check s3
		ISdsBackingStore store = getStore();
		GetRequest gr = new GetRequest();
		gr.mailbox = "titi";
		gr.guid = guid;
		Path tmp = Files.createTempFile("cacaprout" + System.currentTimeMillis(), ".eml");
		gr.filename = tmp.toFile().getAbsolutePath();
		Files.delete(tmp);
		store.download(gr).get(10, TimeUnit.SECONDS);
		byte[] content = Files.readAllBytes(tmp);
		System.err.println("content: " + content.length);
		System.err.println("emldata: " + emlData.length);
		assertTrue(Arrays.equals(content, emlData));

		MgetRequest mget = new MgetRequest();
		mget.mailbox = "titi";
		mget.transfers = new ArrayList<>(200);
		for (int i = 0; i < 200; i++) {
			Path get = Files.createTempFile("mget", ".eml");
			mget.transfers.add(Transfer.of(guid, get.toFile().getAbsolutePath()));
			get.toFile().delete();
		}
		System.err.println("*** mget starts ***");
		int CNT = 20;
		for (int i = 0; i < CNT; i++) {
			long time = System.currentTimeMillis();
			store.downloads(mget).get(10, TimeUnit.SECONDS);
			time = System.currentTimeMillis() - time;
			System.err.println("*** mget in " + time + "ms.");
		}
		for (Transfer t : mget.transfers) {
			File f = new File(t.filename);
			content = Files.readAllBytes(f.toPath());
			assertTrue(Arrays.equals(content, emlData));
			f.delete();
		}

	}

	private static class ToDeliver {
		private byte[] emlData;
		private String sha1;

		@SuppressWarnings("deprecation")
		public ToDeliver() {
			StringBuilder eml = new StringBuilder("From: john" + System.currentTimeMillis() + "@junit.test\r\n\r\n");
			ThreadLocalRandom rand = ThreadLocalRandom.current();
			int lim = 50 * (1024 + rand.nextInt(2048));
			for (int i = 0; i < lim; i++) {
				eml.append("aa");
			}
			eml.append("\r\n" + UUID.randomUUID().toString() + "\r\n");
			this.emlData = eml.toString().getBytes();
			this.sha1 = Hashing.sha1().hashBytes(emlData).toString();
		}
	}

	@Test
	public void ensureReplicatedBodyIsFine()
			throws InterruptedException, ExecutionException, TimeoutException, IOException {
		try (SdsProxyManager sdsMgmt = new SdsProxyManager(VertxPlatform.getVertx(), cyrusIp)) {
			sdsMgmt.applyConfiguration(config.asJson()).get(5, TimeUnit.SECONDS);
		}

		// prep emails
		int cnt = 50;
		List<ToDeliver> mails = new ArrayList<>(cnt);
		for (int i = 0; i < cnt; i++) {
			ToDeliver msg = new ToDeliver();
			mails.add(msg);
			System.err.println("Prep " + i + " => " + msg.emlData.length);
		}
		try (StoreClient sc = new StoreClient(cyrusIp, 1143, userUid + "@" + domainUid, userUid)) {
			sc.login();
			for (ToDeliver del : mails) {
				int added = sc.append("INBOX", new ByteArrayInputStream(del.emlData), new FlagsList());
				assertTrue(added > 0);
				System.err.println("Delivered " + added);
			}
		}
		IDbMessageBodies bodyApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDbMessageBodies.class, partition.name);

		System.err.println("Waiting, partition is " + partition.name);
		for (ToDeliver del : mails) {
			MessageBody fetched = bodyApi.getComplete(del.sha1);
			while (fetched == null) {
				Thread.sleep(100);
				System.err.println("recheck " + del.sha1);
				fetched = bodyApi.getComplete(del.sha1);
			}
			System.err.println("fetched " + del.sha1 + " " + fetched);
			assertEquals(del.emlData.length, fetched.size);
		}

	}

}
