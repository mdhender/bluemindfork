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
package net.bluemind.sds.proxy.store.s3.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
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
import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.backend.cyrus.replication.testhelper.CyrusReplicationHelper;
import net.bluemind.backend.cyrus.replication.testhelper.SyncServerHelper;
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
import net.bluemind.sds.proxy.dto.GetRequest;
import net.bluemind.sds.proxy.mgmt.SdsProxyManager;
import net.bluemind.sds.proxy.store.ISdsBackingStore;
import net.bluemind.sds.proxy.store.s3.S3BackingStoreFactory;
import net.bluemind.sds.proxy.testhelper.ObjectStoreTestHelper;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.vertx.testhelper.Deploy;

public class SdsProxyWithS3IntegrationTests {

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("es.mailspool.count", "1");
	}

	private CyrusReplicationHelper cyrusReplication;
	private String domainUid;
	private String userUid;
	private String cyrusIp;
	private String bucket;
	private S3Configuration config;

	@Before
	public void before() throws Exception {

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

		ItemValue<Server> cyrusServer = ItemValue.create("localhost", imapServer);
		CyrusService cyrusService = new CyrusService(cyrusServer);
		cyrusService.reset();

		PopulateHelper.initGlobalVirt(esServer, imapServer);
		ElasticsearchTestHelper.getInstance().beforeTest();
		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.none);

		String unique = "" + System.currentTimeMillis();
		domainUid = "test" + unique + ".lab";
		userUid = "user" + unique;

		// ensure the partition is created correctly before restarting cyrus
		PopulateHelper.addDomain(domainUid, Routing.none);

		ObjectStoreTestHelper.setup(cyrusService, false);

		System.err.println("Setup replication START");
		this.cyrusReplication = new CyrusReplicationHelper(cyrusIp);
		cyrusReplication.installReplication();
		System.err.println("Setup replication END");

		JdbcActivator.getInstance().addMailboxDataSource(cyrusReplication.server().uid,
				JdbcTestHelper.getInstance().getMailboxDataDataSource());

		CountDownLatch cdl = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(ar -> {
			cdl.countDown();
		});
		boolean beforeTimeout = cdl.await(30, TimeUnit.SECONDS);
		assertTrue(beforeTimeout);

		MQ.init().get(30, TimeUnit.SECONDS);
		Topology.get();

		SyncServerHelper.waitFor();

		cyrusReplication.startReplication().get(5, TimeUnit.SECONDS);

		this.bucket = "junit-" + System.currentTimeMillis();
		this.config = S3Configuration.withEndpointAndBucket("http://" + DockerEnv.getIp("bluemind/s3") + ":8000",
				bucket);

		ImmutableMap<String, String> freshConf = ImmutableMap.of(SysConfKeys.archive_kind.name(), "s3", //
				SysConfKeys.sds_s3_access_key.name(), config.getAccessKey(), //
				SysConfKeys.sds_s3_secret_key.name(), config.getSecretKey(), //
				SysConfKeys.sds_s3_endpoint.name(), config.getEndpoint(), //
				SysConfKeys.sds_s3_bucket.name(), config.getBucket());
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		ISystemConfiguration sysConfApi = prov.instance(ISystemConfiguration.class);
		sysConfApi.updateMutableValues(freshConf);

		System.err.println("Start populate user " + userUid);
		PopulateHelper.addUser(userUid, domainUid, Routing.internal);
	}

	@After
	public void after() throws Exception {
		System.err.println("Waiting for last events (remove this sleep ?)...");
		Thread.sleep(1000);
		cyrusReplication.stopReplication().get(5, TimeUnit.SECONDS);
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void configureSdsProxy() throws InterruptedException, ExecutionException, TimeoutException, IOException {
		// configure sds-proxy for s3

		try (SdsProxyManager sdsMgmt = new SdsProxyManager(VertxPlatform.getVertx(), cyrusIp)) {
			sdsMgmt.applyConfiguration(config.asJson()).get(5, TimeUnit.SECONDS);
		}

		// append mail
		String eml = "From: john" + System.currentTimeMillis() + "@junit.test\r\n";
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
		ISdsBackingStore s3 = new S3BackingStoreFactory().create(VertxPlatform.getVertx(), config.asJson());
		GetRequest gr = new GetRequest();
		gr.mailbox = "titi";
		gr.guid = guid;
		Path tmp = Files.createTempFile("toto" + System.currentTimeMillis(), ".eml");
		gr.filename = tmp.toFile().getAbsolutePath();
		s3.download(gr);
		byte[] content = Files.readAllBytes(tmp);
		assertTrue(Arrays.equals(content, emlData));

	}

}
