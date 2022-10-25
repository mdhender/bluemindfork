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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.Vertx;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.addressbook.api.VCard.Kind;
import net.bluemind.backend.cyrus.replication.observers.IReplicationObserver;
import net.bluemind.backend.cyrus.replication.observers.IReplicationObserverProvider;
import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.api.Email;
import net.bluemind.core.backup.continuous.DefaultBackupStore;
import net.bluemind.core.backup.continuous.leader.DefaultLeader;
import net.bluemind.core.container.api.IOfflineMgmt;
import net.bluemind.core.container.api.IdRange;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.BaseDirEntry.AccountType;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.Server;
import net.bluemind.system.state.RunningState;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class UserDefaultAliasTests {

	private String domainUid;
	private String routingUid;
	private ItemValue<Server> backend;
	private String alias;

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
		try {
			beforeImpl();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	public void beforeImpl() throws Exception {
		DefaultLeader.reset();
		DefaultBackupStore.reset();

		JdbcTestHelper.getInstance().beforeTest();

		Server imapServer = new Server();
		imapServer.tags = Collections.singletonList("mail/imap");
		imapServer.ip = PopulateHelper.FAKE_CYRUS_IP;

		Server esServer = Server.tagged(ElasticsearchTestHelper.getInstance().getHost(), "bm/es");
		assertNotNull(esServer.ip);

		System.err.println("populate global virt...");
		StateContext.setInternalState(new RunningState());
		PopulateHelper.initGlobalVirt(imapServer, esServer);
		ElasticsearchTestHelper.getInstance().beforeTest();
		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.none);

		VertxPlatform.spawnBlocking(20, TimeUnit.SECONDS);
		this.domainUid = "dom" + System.currentTimeMillis() + ".lan";
		this.alias = domainUid.replace("dom", "alias");

		PopulateHelper.addDomain(domainUid, Routing.none, alias);

		await().atMost(20, TimeUnit.SECONDS).until(() -> Topology.getIfAvailable().isPresent());
		this.backend = Topology.get().any("mail/imap");

		this.routingUid = PopulateHelper.addUser("routing", domainUid);
		await().atMost(20, TimeUnit.SECONDS).until(() -> userInbox(routingUid).isPresent());

	}

	private Optional<ItemValue<MailboxReplica>> userInbox(String uid) {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IDbByContainerReplicatedMailboxes foldersApi = prov.instance(IDbByContainerReplicatedMailboxes.class,
				IMailReplicaUids.subtreeUid(domainUid, Mailbox.Type.user, uid));
		return Optional.ofNullable(foldersApi.byReplicaName("INBOX"));
	}

	@Test
	public void defaultEmailOnAlias() throws Exception {
		User user = new User();
		user.login = "dingo";
		user.password = "dingo";
		user.dataLocation = backend.uid;
		user.routing = Routing.internal;
		user.accountType = AccountType.FULL;
		VCard card = new VCard();
		card.kind = Kind.individual;
		card.identification.name = Name.create("wick", "john", null, null, null, null);
		user.contactInfos = card;

		List<Email> emails = Arrays.asList(Email.create("dingo@" + domainUid, false, false),
				Email.create("john@" + alias, true, false));
		Collections.reverse(emails);
		user.emails = emails;

		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IUser userApi = prov.instance(IUser.class, domainUid);
		IOfflineMgmt alloc = prov.instance(IOfflineMgmt.class, domainUid, routingUid);
		IdRange fresh = alloc.allocateOfflineIds(1);
		ItemValue<User> asItem = ItemValue.create("dingo_uid", user);
		asItem.internalId = fresh.globalCounter;
		asItem.version = 1;
		System.err.println("Id should be " + asItem.internalId);

		userApi.restore(asItem, true);

		await().atMost(20, TimeUnit.SECONDS).until(() -> userInbox(asItem.uid).isPresent());

		ItemValue<User> fetched = userApi.getComplete("dingo_uid");
		System.err.println("email: " + fetched.value.defaultEmailAddress());
		assertEquals("john@" + alias, fetched.value.defaultEmailAddress());
		assertEquals(fresh.globalCounter, fetched.internalId);
		IMailboxes mboxApi = prov.instance(IMailboxes.class, domainUid);
		ItemValue<Mailbox> mbox = mboxApi.getComplete("dingo_uid");
		System.err.println("mbox: " + mbox.value + " e: " + mbox.value.defaultEmail());

		IDirectory dirApi = prov.instance(IDirectory.class, domainUid);
		DirEntry dirEntry = dirApi.findByEntryUid("dingo_uid");
		System.err.println("de: " + dirEntry);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

}
