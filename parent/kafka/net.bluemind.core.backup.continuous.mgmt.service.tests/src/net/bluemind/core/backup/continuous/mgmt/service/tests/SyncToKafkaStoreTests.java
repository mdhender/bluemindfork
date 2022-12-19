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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.IntStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import io.netty.util.AsciiString;
import net.bluemind.central.reverse.proxy.model.common.mapper.RecordKey;
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
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.delivery.lmtp.ApiProv;
import net.bluemind.delivery.lmtp.LmtpMessageHandler;
import net.bluemind.delivery.lmtp.dedup.DuplicateDeliveryDb;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.kafka.container.ZkKafkaContainer;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class SyncToKafkaStoreTests {

	private ZkKafkaContainer kafka;
	private String domainUid;
	private String user1;
	private String user1mail;
	private String user2;
	private String user2mail;
	private String johnUid;
	private String janeUid;
	private SecurityContext defaultSecurityContext;

	private static DuplicateDeliveryDb dedup = DuplicateDeliveryDb.get();

	@Before
	public void before() throws Exception {
		this.kafka = new ZkKafkaContainer();
		kafka.start();
		String ip = kafka.inspectAddress();
		System.setProperty("bm.kafka.bootstrap.servers", ip + ":9093");
		System.setProperty("bm.zk.servers", ip + ":2181");
		System.setProperty("node.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);
		System.setProperty("imap.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);

		domainUid = "devenv.blue";
		user1 = "john01";
		user1mail = user1 + "@" + domainUid;
		user2 = "jane01";
		user2mail = user2 + "@" + domainUid;

		DefaultLeader.reset();

		JdbcTestHelper.getInstance().beforeTest();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		VertxPlatform.spawnBlocking(10, TimeUnit.SECONDS);

		BmConfIni ini = new BmConfIni();

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		System.out.println("ES is " + esServer.ip);
		assertNotNull(esServer.ip);
		esServer.tags = Collections.singletonList("bm/es");

		Server imapServer = new Server();
		imapServer.ip = PopulateHelper.FAKE_CYRUS_IP;
		imapServer.tags = Collections.singletonList("mail/imap");

		await().atMost(20, TimeUnit.SECONDS).until(DefaultLeader.leader()::isLeader);
		PopulateHelper.initGlobalVirt(esServer, imapServer);
		ElasticsearchTestHelper.getInstance().beforeTest();

		System.err.println("Add global.virt");
		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.none);

		System.err.println("Pause store then init domain and john01");
		DefaultBackupStore.store().pause();

		System.err.println("Add " + domainUid);
		PopulateHelper.addDomain(domainUid, Routing.none, "devenv.bm");
		System.err.println(domainUid + " added.");

		johnUid = PopulateHelper.addUser(user1, domainUid, Routing.internal);
		assertNotNull(johnUid);
		System.err.println("Add " + user1 + " returned.");

		defaultSecurityContext = BmTestContext
				.contextWithSession("testUser", user1, domainUid, SecurityContext.ROLE_SYSTEM).getSecurityContext();
		BmTestContext context = new BmTestContext(defaultSecurityContext);

		String grpUid = PopulateHelper.addGroup(domainUid, "brotherhood", "confrérie",
				Collections.singletonList(Member.user(johnUid)));

		IGroup groupApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domainUid);
		ItemValue<Group> brotherhood = groupApi.getComplete(grpUid);
		System.err.println("group is " + brotherhood);

		System.err.println("Resume store then create " + user2);

		janeUid = PopulateHelper.addUser(user2, domainUid, Routing.internal);
		assertNotNull(janeUid);
		System.err.println("Add " + user2 + " returned.");
		fillMailbox(context, user1mail, user2mail, 5);
		fillMailbox(context, user2mail, user1mail, 3);

		System.err.println("Add " + user2 + " the group");
		DefaultBackupStore.store().resume();
		groupApi.add(grpUid, Collections.singletonList(Member.user(janeUid)));
	}

	private void fillMailbox(BmTestContext context, String from, String to, int imax)
			throws InterruptedException, ExecutionException, TimeoutException, IOException {
		ApiProv prov = k -> context.getServiceProvider();
		LmtpMessageHandler messageHandler = new LmtpMessageHandler(prov, dedup);
		IntStream.range(0, imax).forEach(i -> {
			StringBuilder sb = new StringBuilder();
			sb.append("From: ").append(from).append("\r\n");
			sb.append("Message-Id: " + UUID.randomUUID().toString()).append("\r\n");
			sb.append("Subject: yeah " + UUID.randomUUID().toString()).append("\r\n");
			sb.append("Content-Type: text/plain\r\n");
			sb.append("\r\n");
			sb.append(System.currentTimeMillis());
			sb.append("\r\n");
			String eml = sb.toString();
			InputStream targetStream = new ByteArrayInputStream(eml.getBytes());
			try {
				messageHandler.deliver(from, to, targetStream);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
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
		ObjectMapper objectMapper = new ObjectMapper();
		ObjectReader objectReader = objectMapper.readerFor(RecordKey.class);
		List<RecordKey> recordKeys = new ArrayList<>();
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

		TopicSubscriber devEnvSub = kts.getSubscriber("sync" + "noid" + "-" + domainUid);
		LongAdder syncDom = new LongAdder();
		devEnvSub.subscribe((byte[] k, byte[] v, int part, long off) -> {
			CharSequence key = new AsciiString(k);
			String keyString = key.toString();
			try {
				RecordKey recordKey = objectReader.readValue(keyString);
				recordKeys.add(recordKey);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
			System.err.println(" * " + key);
			syncDom.increment();
		});
		assertTrue("We expected records in sync " + domainUid + " topic", syncDom.sum() > 0);

		ILiveBackupStreams streams = DefaultBackupStore.reader().forInstallation("sync" + "noid");
		List<ILiveStream> avail = streams.listAvailable();
		for (ILiveStream ls : avail) {
			System.err.println(" * reader has " + ls);
		}
		assertEquals(2, avail.size());

		long user1CountMailboxRecords = recordKeys.stream()
				.filter(k -> k.owner.equals(user1) && k.type.equals("mailbox_records")).count();
		assertEquals(3L, user1CountMailboxRecords);

		long user2CountMailboxRecords = recordKeys.stream()
				.filter(k -> k.owner.equals(user2) && k.type.equals("mailbox_records")).count();
		assertEquals(5L, user2CountMailboxRecords);

		long user1CountMessageBodies = recordKeys.stream()
				.filter(k -> k.owner.equals(user1) && k.type.equals("message_bodies")).count();
		assertEquals(3L, user1CountMessageBodies);

		long user2CountMessageBodies = recordKeys.stream()
				.filter(k -> k.owner.equals(user2) && k.type.equals("message_bodies")).count();
		assertEquals(5L, user2CountMessageBodies);

		long user1CountIndexedMessageBodies = recordKeys.stream()
				.filter(k -> k.owner.equals(user1) && k.type.equals("message_bodies_es_source")).count();
		assertEquals(3L, user1CountIndexedMessageBodies);

		long user2CountIndexedMessageBodies = recordKeys.stream()
				.filter(k -> k.owner.equals(user2) && k.type.equals("message_bodies_es_source")).count();
		assertEquals(5L, user2CountIndexedMessageBodies);
	}

	protected IServiceProvider systemServiceProvider() {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
	}
}
