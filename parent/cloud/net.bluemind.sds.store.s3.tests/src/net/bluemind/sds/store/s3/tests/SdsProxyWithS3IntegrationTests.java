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
package net.bluemind.sds.store.s3.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.bluemind.aws.s3.utils.S3Configuration;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.dockerclient.DockerEnv;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.StoreClient;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.network.topology.Topology;
import net.bluemind.sds.dto.GetRequest;
import net.bluemind.sds.dto.MgetRequest;
import net.bluemind.sds.dto.MgetRequest.Transfer;
import net.bluemind.sds.store.ISdsBackingStore;
import net.bluemind.sds.store.s3.S3StoreFactory;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.state.RunningState;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class SdsProxyWithS3IntegrationTests {
	private String domainUid;
	private String userUid;
	private String bucket;
	private S3Configuration config;
	private CyrusPartition partition;

	@BeforeClass
	public static void sysprop() {
		System.setProperty("node.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);
		System.setProperty("imap.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);
		System.setProperty("imap.port", "1143");
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		Server pipo = new Server();
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;
		pipo.tags = Collections.singletonList("mail/imap");

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		System.out.println("ES is " + esServer.ip);
		assertNotNull(esServer.ip);
		esServer.tags = Lists.newArrayList("bm/es");

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		PopulateHelper.initGlobalVirt(pipo);
		ElasticsearchTestHelper.getInstance().beforeTest();
		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.none);

		String unique = "" + System.currentTimeMillis();
		domainUid = "test" + unique + ".lab";
		userUid = "user" + unique;

		PopulateHelper.addDomain(domainUid, Routing.none);

		MQ.init().get(30, TimeUnit.SECONDS);
		Topology.get();

		this.partition = CyrusPartition.forServerAndDomain(Topology.get().any("mail/imap"), domainUid);

		this.bucket = "junit-" + System.currentTimeMillis();
		this.config = S3Configuration.withEndpointAndBucket("http://" + DockerEnv.getIp("bluemind/s3") + ":8000",
				bucket);

		ImmutableMap<String, String> freshConf = new ImmutableMap.Builder<String, String>() //
				.put(SysConfKeys.archive_kind.name(), "s3") //
				.put(SysConfKeys.sds_s3_access_key.name(), config.getAccessKey()) //
				.put(SysConfKeys.sds_s3_secret_key.name(), config.getSecretKey()) //
				.put(SysConfKeys.sds_s3_endpoint.name(), config.getEndpoint()) //
				.put(SysConfKeys.sds_s3_region.name(), config.getRegion()) //
				.put(SysConfKeys.sds_s3_bucket.name(), config.getBucket()) //
				.build();
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		ISystemConfiguration sysConfApi = prov.instance(ISystemConfiguration.class);
		sysConfApi.updateMutableValues(freshConf);

		StateContext.setInternalState(new RunningState());

		System.err.println("Start populate user " + userUid);
		PopulateHelper.addUser(userUid, domainUid, Routing.internal);
		System.err.println("Populated.");
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void configureSdsProxy() throws InterruptedException, ExecutionException, TimeoutException, IOException {
		// append mail
		String eml = "From: john" + System.currentTimeMillis() + "@junit.test\r\n\r\n";
		for (int i = 0; i < 50 * 1014; i++) {
			eml += "aa";
		}
		byte[] emlData = eml.getBytes();
		@SuppressWarnings("deprecation")
		ByteBuf hash = Unpooled.wrappedBuffer(Hashing.sha1().hashBytes(emlData).asBytes());
		String guid = ByteBufUtil.hexDump(hash);
		System.err.println("Body guid should be " + guid);
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, userUid + "@" + domainUid, userUid)) {
			assertTrue(sc.login());
			int added = sc.append("INBOX", new ByteArrayInputStream(emlData), new FlagsList());
			assertTrue(added > 0);
		}

		// check s3
		ISdsBackingStore s3 = new S3StoreFactory().create(VertxPlatform.getVertx(), config.asJson());
		GetRequest gr = new GetRequest();
		gr.mailbox = "titi";
		gr.guid = guid;
		Path tmp = Files.createTempFile("toto" + System.currentTimeMillis(), ".eml");
		gr.filename = tmp.toFile().getAbsolutePath();
		Files.delete(tmp);
		s3.download(gr).get(10, TimeUnit.SECONDS);
		byte[] content = Files.readAllBytes(tmp);
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
			s3.downloads(mget).get(10, TimeUnit.SECONDS);
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
		// prep emails
		int cnt = 50;
		List<ToDeliver> mails = new ArrayList<>(cnt);
		for (int i = 0; i < cnt; i++) {
			ToDeliver msg = new ToDeliver();
			mails.add(msg);
			System.err.println("Prep " + i + " => " + msg.emlData.length);
		}
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, userUid + "@" + domainUid, userUid)) {
			assertTrue(sc.login());
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
