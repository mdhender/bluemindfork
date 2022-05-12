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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.backup.continuous.tests;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.hash.Hashing;

import io.vertx.core.Vertx;
import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.cyrus.replication.client.ReplMailbox;
import net.bluemind.backend.cyrus.replication.client.ReplMailbox.Builder;
import net.bluemind.backend.cyrus.replication.client.SyncClientOIO;
import net.bluemind.backend.cyrus.replication.observers.IReplicationObserver;
import net.bluemind.backend.cyrus.replication.observers.IReplicationObserverProvider;
import net.bluemind.backend.cyrus.replication.testhelper.CyrusReplicationHelper;
import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.network.topology.Topology;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class PushRecordsTests {

	private String cyrusIp;
	private CyrusReplicationHelper replicationHelper;
	private String domainUid;
	private String userUid;
	private ItemValue<Server> backend;

	public static class ApplyWatch implements IReplicationObserverProvider, IReplicationObserver {

		public static final Map<String, AtomicLong> counts = new ConcurrentHashMap<>();

		@Override
		public IReplicationObserver create(Vertx vertx) {
			return this;
		}

		@Override
		public void onApplyMessages(int total) {
		}

		public static long count(String mboxUniqueId) {
			return counts.computeIfAbsent(mboxUniqueId, k -> new AtomicLong()).get();
		}

		@Override
		public void onApplyMailbox(String mboxUniqueId, long lastUid) {
			AtomicLong adder = counts.computeIfAbsent(mboxUniqueId, k -> new AtomicLong());
			adder.incrementAndGet();
		}

	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		this.cyrusIp = new BmConfIni().get("imap-role");

		Server imapServer = Server.tagged(cyrusIp, "mail/imap");

		Server esServer = Server.tagged(ElasticsearchTestHelper.getInstance().getHost(), "bm/es");
		assertNotNull(esServer.ip);

		this.replicationHelper = new CyrusReplicationHelper(cyrusIp);
		String coreIp = CyrusReplicationHelper.getMyIpAddress();
		System.setProperty("sync.core.address", coreIp);
		System.err.println("coreIp for replication set to " + coreIp);

		System.err.println("populate global virt...");
		PopulateHelper.initGlobalVirt(imapServer, esServer);
		ElasticsearchTestHelper.getInstance().beforeTest();
		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.none);

		VertxPlatform.spawnBlocking(20, TimeUnit.SECONDS);
		System.err.println("Waiting for SyncServer...");
		SyncServerHelper.waitFor();
		System.err.println("=======================");
		this.domainUid = "dom" + System.currentTimeMillis() + ".lan";

		new CyrusService(cyrusIp).reset();

		PopulateHelper.addDomain(domainUid, Routing.none);

		replicationHelper.installReplication();
		replicationHelper.startReplication().get(10, TimeUnit.SECONDS);

		this.userUid = PopulateHelper.addUser("john", domainUid, Routing.internal);

		await().atMost(20, TimeUnit.SECONDS).until(() -> Topology.getIfAvailable().isPresent());
		await().atMost(20, TimeUnit.SECONDS).until(() -> userInbox().isPresent());
		this.backend = Topology.get().any("mail/imap");

	}

	Optional<ItemValue<MailboxReplica>> userInbox() {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IDbByContainerReplicatedMailboxes foldersApi = prov.instance(IDbByContainerReplicatedMailboxes.class,
				IMailReplicaUids.subtreeUid(domainUid, Mailbox.Type.user, userUid));
		return Optional.ofNullable(foldersApi.byReplicaName("INBOX"));
	}

	@Test
	public void pushRecords() throws Exception {
		try (SyncClientOIO client = new SyncClientOIO(cyrusIp, 2502)) {
			client.authenticate("admin0", "admin");
			ItemValue<MailboxReplica> inbox = userInbox()
					.orElseThrow(() -> new NullPointerException("inbox must not be null"));

			byte[] emlData = "From: john@bm.lan\r\n".getBytes();
			@SuppressWarnings("deprecation")
			String bodyGuid = Hashing.sha1().hashBytes(emlData).toString();// NOSONAR

			String result = client.applyMessage(part().name, bodyGuid, emlData);
			assertEquals("OK success", result);

			AtomicLong count = new AtomicLong(ApplyWatch.count(inbox.uid));
			String apply = client.applyMailbox(prepareApplyMailbox(inbox, 200L, 42L).build());
			System.err.println("apply lastuid 42 modseq 200 no recs: " + apply);
			await().atMost(5, TimeUnit.SECONDS).until(() -> ApplyWatch.count(inbox.uid) > count.get());

			count.set(ApplyWatch.count(inbox.uid));
			apply = client.applyMailbox(prepareApplyMailbox(inbox, 200L, 32L).build(),
					preparereRecord(32L, 100, bodyGuid, emlData.length));
			System.err.println("apply uid 32 modseq 100 " + apply);
			await().atMost(5, TimeUnit.SECONDS).until(() -> ApplyWatch.count(inbox.uid) > count.get());
		}
	}

	private CyrusPartition part() {
		return CyrusPartition.forServerAndDomain(backend, domainUid);
	}

	private Builder prepareApplyMailbox(ItemValue<MailboxReplica> inbox, Long modSeq, Long lastUid) {
		Builder builder = ReplMailbox.builder();
		inbox.value.acls.stream().forEach(acl -> builder.acl(acl.subject, new net.bluemind.imap.Acl(acl.rights)));
		builder//
				.uniqueId(UUID.fromString(inbox.uid))//
				.mailboxName("john")//
				.root().folderName("INBOX")//
				.domainUid(domainUid)//
				.mailboxUid(userUid)//
				.partition(part())//
				.lastUid(Optional.ofNullable(lastUid).orElse(inbox.value.lastUid))//
				.highestModSeq(Optional.ofNullable(modSeq).orElse(inbox.value.highestModSeq))//
				.lastAppendDate(inbox.value.lastAppendDate)//
				.uidValidity(inbox.value.uidValidity);
		return builder;
	}

	private String preparereRecord(long imapUid, int modSeq, String bodyGuid, int size) {
		// %(UID 1 MODSEQ 305 LAST_UPDATED 1619172582 FLAGS () INTERNALDATE 1619169573
		// SIZE 54 GUID 3a6785fe8081d403c6721ae8637c0016db7963f8)
		long date = System.currentTimeMillis() / 1000L;
		StringBuilder recordsBuffer = new StringBuilder();
		recordsBuffer.append("%(");
		recordsBuffer.append("UID ").append(imapUid);
		recordsBuffer.append(" MODSEQ ").append(modSeq);
		recordsBuffer.append(" LAST_UPDATED ").append(date);
		recordsBuffer.append(" FLAGS ()");
		recordsBuffer.append(" INTERNALDATE ").append(date);
		recordsBuffer.append(" SIZE ").append(size);
		recordsBuffer.append(" GUID " + bodyGuid).append(")");

		return recordsBuffer.toString();
	}

	@After
	public void after() throws Exception {
		if (replicationHelper != null) {
			replicationHelper.stopReplication().get(10, TimeUnit.SECONDS);
		}
		JdbcTestHelper.getInstance().afterTest();
	}

}
