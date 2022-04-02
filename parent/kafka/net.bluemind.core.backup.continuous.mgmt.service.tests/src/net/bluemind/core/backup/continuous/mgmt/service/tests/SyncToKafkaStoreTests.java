/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.core.backup.continuous.mgmt.service.tests;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.netty.util.AsciiString;
import net.bluemind.backend.cyrus.replication.testhelper.CyrusReplicationHelper;
import net.bluemind.backend.cyrus.replication.testhelper.ExpectCommand;
import net.bluemind.backend.cyrus.replication.testhelper.SyncServerHelper;
import net.bluemind.config.Token;
import net.bluemind.core.backup.continuous.DefaultBackupStore;
import net.bluemind.core.backup.continuous.ILiveBackupStreams;
import net.bluemind.core.backup.continuous.ILiveStream;
import net.bluemind.core.backup.continuous.leader.DefaultLeader;
import net.bluemind.core.backup.continuous.mgmt.api.BackupSyncOptions;
import net.bluemind.core.backup.continuous.mgmt.api.IContinuousBackupMgmt;
import net.bluemind.core.backup.continuous.store.TopicSubscriber;
import net.bluemind.core.backup.store.kafka.KafkaTopicStore;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.ListInfo;
import net.bluemind.imap.StoreClient;
import net.bluemind.kafka.container.ZkKafkaContainer;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class SyncToKafkaStoreTests {

	private ZkKafkaContainer kafka;
	private String cyrusIp;
	private CyrusReplicationHelper cyrusReplication;

	@Before
	public void before() throws Exception {
		this.kafka = new ZkKafkaContainer();
		kafka.start();
		String ip = kafka.inspectAddress();
		System.setProperty("bm.kafka.bootstrap.servers", ip + ":9093");
		System.setProperty("bm.zk.servers", ip + ":2181");

		DefaultLeader.reset();

		JdbcTestHelper.getInstance().beforeTest();

		VertxPlatform.spawnBlocking(10, TimeUnit.SECONDS);

		await().atMost(20, TimeUnit.SECONDS).until(DefaultLeader.leader()::isLeader);

		BmConfIni ini = new BmConfIni();

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		System.out.println("ES is " + esServer.ip);
		assertNotNull(esServer.ip);
		esServer.tags = Collections.singletonList("bm/es");

		this.cyrusIp = ini.get("imap-role");
		Server imapServer = new Server();
		imapServer.ip = cyrusIp;
		imapServer.tags = Collections.singletonList("mail/imap");

		PopulateHelper.initGlobalVirt(esServer, imapServer);
		ElasticsearchTestHelper.getInstance().beforeTest();

		System.err.println("Add global.virt");
		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.none);

		System.err.println("Pause store then init domain and john01");
		DefaultBackupStore.store().pause();

		System.err.println("Add devenv.blue");
		PopulateHelper.addDomain("devenv.blue", Routing.none, "devenv.bm");
		System.err.println("devenv.blue added.");

		this.cyrusReplication = new CyrusReplicationHelper(cyrusIp);
		System.err.println("Replication setup starts.");
		cyrusReplication.installReplication();
		SyncServerHelper.waitFor();
		cyrusReplication.startReplication().get(5, TimeUnit.SECONDS);

		ExpectCommand expect = new ExpectCommand();

		String johnUid = PopulateHelper.addUser("john01", "devenv.blue", Routing.internal);
		System.err.println("Add john01 returned.");

		fillMailbox(expect, "john01@devenv.blue");

		String grpUid = PopulateHelper.addGroup("devenv.blue", "brotherhood", "confrérie",
				Collections.singletonList(Member.user(johnUid)));

		IGroup groupApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				"devenv.blue");
		ItemValue<Group> brotherhood = groupApi.getComplete(grpUid);
		System.err.println("group is " + brotherhood);

		System.err.println("Resume store then create jane01");
		DefaultBackupStore.store().resume();

		String janeUid = PopulateHelper.addUser("jane01", "devenv.blue", Routing.internal);
		System.err.println("Add jane01 returned.");
		fillMailbox(expect, "jane01@devenv.blue");

		System.err.println("Add jane the group");
		groupApi.add(grpUid, Collections.singletonList(Member.user(janeUid)));
	}

	private void fillMailbox(ExpectCommand expect, String latd)
			throws InterruptedException, ExecutionException, TimeoutException {
		try (StoreClient sc = new StoreClient(cyrusIp, 1143, latd, "yeah")) {
			assertTrue(sc.login());
			for (ListInfo lr : sc.listAll()) {
				if (lr.isSelectable()) {
					String eml = "From: wick" + UUID.randomUUID().toString() + "@gmail.com\r\n"//
							+ "Subject: yeah " + UUID.randomUUID().toString() + "\r\n"//
							+ "\r\n";
					CompletableFuture<Void> freshMsg = expect.onNextApplyMessage();
					int uid = sc.append(lr.getName(), new ByteArrayInputStream(eml.getBytes()), new FlagsList());
					assertTrue(uid > 0);
					freshMsg.get(2, TimeUnit.SECONDS);
				}
			}
		}
	}

	@After
	public void after() throws Exception {
		System.err.println("after starts.");
		DefaultLeader.leader().releaseLeadership();
		kafka.stop();

		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testTaskRef() {
		ClientSideServiceProvider csp = ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", Token.admin0());
		IContinuousBackupMgmt contApi = csp.instance(IContinuousBackupMgmt.class);

		System.err.println("***** Starting sync process *****");
		TaskRef taskRef = contApi.syncWithStore(new BackupSyncOptions());
		AtomicBoolean noopStoreUsed = new AtomicBoolean();
		TaskStatus status = TaskUtils.wait(csp, taskRef, logEntry -> {
			if (logEntry != null) {
				System.err.println(logEntry);
				if (logEntry.contains("continuous.NoopStore")) {
					noopStoreUsed.set(true);
				}
			}
		});
		System.err.println("status: " + status);

		assertFalse("NoopStore should not be used, we want kafka stuff.", noopStoreUsed.get());

		KafkaTopicStore kts = new KafkaTopicStore();
		TopicSubscriber orphanSub = kts.getSubscriber("sync" + "noid" + "-__orphans__");
		LongAdder syncOrphan = new LongAdder();
		orphanSub.subscribe((byte[] k, byte[] v, int part, long off) -> {
			CharSequence key = new AsciiString(k);
			System.err.println(" * " + key);
			syncOrphan.increment();
		});
		assertTrue("We expected records in sync orphans topic", syncOrphan.sum() > 0);

		TopicSubscriber devEnvSub = kts.getSubscriber("sync" + "noid" + "-devenv.blue");
		LongAdder syncDom = new LongAdder();
		devEnvSub.subscribe((byte[] k, byte[] v, int part, long off) -> {
			CharSequence key = new AsciiString(k);
			System.err.println(" * " + key);
			syncDom.increment();
		});
		assertTrue("We expected records in sync devenv.blue topic", syncDom.sum() > 0);

		ILiveBackupStreams streams = DefaultBackupStore.reader().forInstallation("sync" + "noid");
		List<ILiveStream> avail = streams.listAvailable();
		for (ILiveStream ls : avail) {
			System.err.println(" * reader has " + ls);
		}
		assertEquals(2, avail.size());
	}

}
