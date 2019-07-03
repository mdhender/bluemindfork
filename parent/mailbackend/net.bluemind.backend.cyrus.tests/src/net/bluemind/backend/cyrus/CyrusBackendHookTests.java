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
package net.bluemind.backend.cyrus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.collect.Lists;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.config.InstallationId;
import net.bluemind.config.Token;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.container.persistance.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.persistance.DirEntryStore;
import net.bluemind.imap.Acl;
import net.bluemind.imap.StoreClient;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.mailbox.persistance.MailboxStore;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class CyrusBackendHookTests {

	private String domainUid = "bm.lan";
	private ItemValue<Server> dataLocation;
	private String cyrusIp;
	private ItemStore mailboxItemStore;
	private MailboxStore mailboxStore;
	private ItemStore dirItemStore;
	private DirEntryStore dirStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});
		launched.await();

		cyrusIp = new BmConfIni().get("imap-role");
		assertNotNull(cyrusIp);
		Server imapServer = new Server();
		imapServer.ip = cyrusIp;
		imapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(imapServer);

		IServer serverService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				InstallationId.getIdentifier());

		dataLocation = serverService.getComplete(cyrusIp);

		new CyrusService(cyrusIp).createPartition(domainUid);
		new CyrusService(cyrusIp).refreshPartitions(Arrays.asList(domainUid));
		new CyrusService(cyrusIp).reload();

		PopulateHelper.createTestDomain(domainUid, imapServer);

		ContainerStore containerHome = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM);

		Container mboxContainer = containerHome.get(domainUid);
		mailboxItemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), mboxContainer,
				SecurityContext.SYSTEM);
		mailboxStore = new MailboxStore(JdbcTestHelper.getInstance().getDataSource(), mboxContainer);

		Container dirContainer = containerHome.get(domainUid);
		dirItemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), dirContainer,
				SecurityContext.SYSTEM);
		dirStore = new DirEntryStore(JdbcTestHelper.getInstance().getDataSource(), dirContainer);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testOnUserAclChanged() throws Exception {
		String mailboxUid = UUID.randomUUID().toString();
		Mailbox mailbox = new Mailbox();
		mailbox.name = "mailbox-" + System.nanoTime();
		mailbox.type = Type.user;
		mailbox.archived = false;
		mailbox.dataLocation = dataLocation.uid;
		mailbox.routing = Routing.internal;

		mailboxItemStore.create(Item.create(mailboxUid, null));
		Item mailboxItem = mailboxItemStore.get(mailboxUid);
		mailboxStore.create(mailboxItem, mailbox);

		DirEntry mailboxDirEntry = new DirEntry();
		mailboxDirEntry.entryUid = mailboxUid;
		mailboxDirEntry.kind = Kind.USER;
		mailboxDirEntry.path = "";
		mailboxDirEntry.displayName = mailbox.name;
		mailboxDirEntry.dataLocation = dataLocation.uid;

		Item mailboxDirEntryItem = dirItemStore.get(mailboxUid);
		dirStore.create(mailboxDirEntryItem, mailboxDirEntry);

		String userUid = UUID.randomUUID().toString();
		DirEntry userDirEntry = new DirEntry();
		userDirEntry.entryUid = userUid;
		userDirEntry.kind = Kind.USER;
		userDirEntry.path = "";
		userDirEntry.displayName = "user";
		userDirEntry.dataLocation = dataLocation.uid;

		dirItemStore.create(Item.create(userUid, null));
		Item userDirEntryItem = dirItemStore.get(userUid);
		dirStore.create(userDirEntryItem, userDirEntry);

		String groupUid = UUID.randomUUID().toString();
		DirEntry groupDirEntry = new DirEntry();
		groupDirEntry.entryUid = groupUid;
		groupDirEntry.kind = Kind.GROUP;
		groupDirEntry.path = "";
		groupDirEntry.displayName = "group";
		groupDirEntry.dataLocation = dataLocation.uid;

		dirItemStore.create(Item.create(groupUid, null));
		Item groupDirEntryItem = dirItemStore.get(groupUid);
		dirStore.create(groupDirEntryItem, groupDirEntry);

		// Init mailbox with ACL:
		// - user.login + "@" + domainUid: Acl.ALL
		// - "bronsky@" + domainUid, Acl.ALL
		try (StoreClient sc = new StoreClient(cyrusIp, 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());

			System.err.println("**** Create mailbox: " + mailbox.name + "@" + domainUid);
			sc.createMailbox("user/" + mailbox.name + "@" + domainUid,
					CyrusPartition.forServerAndDomain(dataLocation, domainUid).name);
			sc.setAcl("user/" + mailbox.name + "@" + domainUid, userUid + "@" + domainUid, Acl.ALL);
			sc.setAcl("user/" + mailbox.name + "@" + domainUid, "bronsky@" + domainUid, Acl.ALL);

			sc.setAcl("user/" + mailbox.name + "@" + domainUid, "group:" + groupUid + "@" + domainUid, Acl.ALL);
			sc.setAcl("user/" + mailbox.name + "@" + domainUid, "group:bronski@" + domainUid, Acl.ALL);
		}

		ContainerDescriptor containerDesc = ContainerDescriptor.create("mailbox:acls-" + domainUid, domainUid,
				mailboxUid, null, domainUid, true);

		List<AccessControlEntry> currentAcls = new ArrayList<>();
		currentAcls.add(AccessControlEntry.create(domainUid, Verb.Read));
		currentAcls.add(AccessControlEntry.create(userUid, Verb.Write));
		currentAcls.add(AccessControlEntry.create(groupUid, Verb.Write));

		new CyrusBackendHook().onAclChanged(null, containerDesc, Collections.<AccessControlEntry>emptyList(),
				currentAcls);

		try (StoreClient sc = new StoreClient(cyrusIp, 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());

			Map<String, Acl> acl = sc.listAcl("user/" + mailbox.name + "@" + domainUid);
			assertEquals(5, acl.size());

			int i = 0;
			for (String consumer : acl.keySet()) {
				if (consumer.equals("group:" + domainUid + "@" + domainUid)) {
					assertEquals(Acl.RO.toString(), acl.get(consumer).toString());
					i++;
					continue;
				}

				if (consumer.equals("admin0")) {
					assertEquals(Acl.ALL.toString(), acl.get(consumer).toString());
					i++;
					continue;
				}

				if (consumer.equals(mailboxUid + "@" + domainUid)) {
					assertEquals(Acl.ALL.toString(), acl.get(consumer).toString());
					i++;
					continue;
				}

				if (consumer.equals(userUid + "@" + domainUid)) {
					assertEquals(Acl.RW.toString(), acl.get(consumer).toString());
					i++;
					continue;
				}

				if (consumer.equals("group:" + groupUid + "@" + domainUid)) {
					assertEquals(Acl.RW.toString(), acl.get(consumer).toString());
					i++;
					continue;
				}

				fail("Unknown consumer: " + consumer);
			}

			assertEquals(5, i);
		}

		// Set mailbox implicit ACL to RO instead of ALL and remove admin0 ACE
		try (StoreClient sc = new StoreClient(cyrusIp, 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());

			sc.setAcl("user/" + mailbox.name + "@" + domainUid, mailbox.name + "@" + domainUid, Acl.RO);
			sc.deleteAcl("user/" + mailbox.name + "@" + domainUid, "admin0");
		}

		currentAcls = new ArrayList<>();
		currentAcls.add(AccessControlEntry.create(mailboxUid, Verb.Read));

		new CyrusBackendHook().onAclChanged(null, containerDesc, Collections.<AccessControlEntry>emptyList(),
				currentAcls);

		try (StoreClient sc = new StoreClient(cyrusIp, 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());

			Map<String, Acl> acl = sc.listAcl("user/" + mailbox.name + "@" + domainUid);
			assertEquals(2, acl.size());

			int i = 0;
			for (String consumer : acl.keySet()) {
				if (consumer.equals("admin0")) {
					assertEquals(Acl.ALL.toString(), acl.get(consumer).toString());
					i++;
					continue;
				}

				if (consumer.equals(mailboxUid + "@" + domainUid)) {
					assertEquals(Acl.ALL.toString(), acl.get(consumer).toString());
					i++;
					continue;
				}

				fail("Unknown consumer: " + consumer);
			}

			assertEquals(2, i);
		}
	}

	@Test
	public void testOnMailshareAclChanged() throws Exception {
		String mailboxUid = UUID.randomUUID().toString();
		Mailbox mailbox = new Mailbox();
		mailbox.name = "mailbox-" + System.nanoTime();
		mailbox.type = Type.mailshare;
		mailbox.archived = false;
		mailbox.dataLocation = dataLocation.uid;
		mailbox.routing = Routing.internal;

		mailboxItemStore.create(Item.create(mailboxUid, null));
		Item mailboxItem = mailboxItemStore.get(mailboxUid);
		mailboxStore.create(mailboxItem, mailbox);

		DirEntry mailboxDirEntry = new DirEntry();
		mailboxDirEntry.entryUid = mailboxUid;
		mailboxDirEntry.kind = Kind.MAILSHARE;
		mailboxDirEntry.path = "";
		mailboxDirEntry.displayName = mailbox.name;
		mailboxDirEntry.dataLocation = dataLocation.uid;

		Item mailboxDirEntryItem = dirItemStore.get(mailboxUid);
		dirStore.create(mailboxDirEntryItem, mailboxDirEntry);

		String userUid = UUID.randomUUID().toString();
		DirEntry userDirEntry = new DirEntry();
		userDirEntry.entryUid = userUid;
		userDirEntry.kind = Kind.USER;
		userDirEntry.path = "";
		userDirEntry.displayName = "user";
		userDirEntry.dataLocation = dataLocation.uid;

		dirItemStore.create(Item.create(userUid, null));
		Item userDirEntryItem = dirItemStore.get(userUid);
		dirStore.create(userDirEntryItem, userDirEntry);

		String groupUid = UUID.randomUUID().toString();
		DirEntry groupDirEntry = new DirEntry();
		groupDirEntry.entryUid = groupUid;
		groupDirEntry.kind = Kind.GROUP;
		groupDirEntry.path = "";
		groupDirEntry.displayName = "group";
		groupDirEntry.dataLocation = dataLocation.uid;

		dirItemStore.create(Item.create(groupUid, null));
		Item groupDirEntryItem = dirItemStore.get(groupUid);
		dirStore.create(groupDirEntryItem, groupDirEntry);

		// Init mailbox with ACL:
		// - user.login + "@" + domainUid: Acl.ALL
		// - "bronsky@" + domainUid, Acl.ALL
		try (StoreClient sc = new StoreClient(cyrusIp, 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());

			System.err.println("**** Create mailbox: " + mailbox.name + "@" + domainUid);
			sc.createMailbox(mailbox.name + "@" + domainUid,
					CyrusPartition.forServerAndDomain(dataLocation, domainUid).name);
			sc.setAcl(mailbox.name + "@" + domainUid, userUid + "@" + domainUid, Acl.ALL);
			sc.setAcl(mailbox.name + "@" + domainUid, "bronsky@" + domainUid, Acl.ALL);

			sc.setAcl(mailbox.name + "@" + domainUid, "group:" + groupUid + "@" + domainUid, Acl.ALL);
			sc.setAcl(mailbox.name + "@" + domainUid, "group:bronski@" + domainUid, Acl.ALL);
		}

		ContainerDescriptor containerDesc = ContainerDescriptor.create("mailbox:acls-" + domainUid, domainUid,
				mailboxUid, null, domainUid, true);

		List<AccessControlEntry> currentAcls = new ArrayList<>();
		currentAcls.add(AccessControlEntry.create(domainUid, Verb.Read));
		currentAcls.add(AccessControlEntry.create(userUid, Verb.Write));
		currentAcls.add(AccessControlEntry.create(groupUid, Verb.Write));

		new CyrusBackendHook().onAclChanged(null, containerDesc, Collections.<AccessControlEntry>emptyList(),
				currentAcls);

		try (StoreClient sc = new StoreClient(cyrusIp, 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());

			Map<String, Acl> acl = sc.listAcl(mailbox.name + "@" + domainUid);
			assertEquals(5, acl.size());

			int i = 0;
			for (String consumer : acl.keySet()) {
				if (consumer.equals("group:" + domainUid + "@" + domainUid)) {
					assertEquals(Acl.RO.toString(), acl.get(consumer).toString());
					i++;
					continue;
				}

				if (consumer.equals("admin0")) {
					assertEquals(Acl.ALL.toString(), acl.get(consumer).toString());
					i++;
					continue;
				}

				if (consumer.equals(userUid + "@" + domainUid)) {
					assertEquals(Acl.RW.toString(), acl.get(consumer).toString());
					i++;
					continue;
				}

				if (consumer.equals("group:" + groupUid + "@" + domainUid)) {
					assertEquals(Acl.RW.toString(), acl.get(consumer).toString());
					i++;
					continue;
				}

				if (consumer.equals("anyone")) {
					assertEquals(Acl.POST.toString(), acl.get(consumer).toString());
					i++;
					continue;
				}

				fail("Unknown consumer: " + consumer);
			}

			assertEquals(5, i);
		}

		// Set mailbox implicit ACL to RO instead of ALL and remove admin0 ACE
		try (StoreClient sc = new StoreClient(cyrusIp, 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());

			sc.setAcl(mailbox.name + "@" + domainUid, userUid + "@" + domainUid, Acl.RO);
			sc.deleteAcl(mailbox.name + "@" + domainUid, "admin0");
		}

		currentAcls = new ArrayList<>();
		currentAcls.add(AccessControlEntry.create(userUid, Verb.Read));

		new CyrusBackendHook().onAclChanged(null, containerDesc, Collections.<AccessControlEntry>emptyList(),
				currentAcls);

		try (StoreClient sc = new StoreClient(cyrusIp, 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());

			Map<String, Acl> acl = sc.listAcl(mailbox.name + "@" + domainUid);
			assertEquals(3, acl.size());

			int i = 0;
			for (String consumer : acl.keySet()) {
				if (consumer.equals("admin0")) {
					assertEquals(Acl.ALL.toString(), acl.get(consumer).toString());
					i++;
					continue;
				}

				if (consumer.equals("anyone")) {
					assertEquals(Acl.POST.toString(), acl.get(consumer).toString());
					i++;
					continue;
				}

				if (consumer.equals(userUid + "@" + domainUid)) {
					assertEquals(Acl.RO.toString(), acl.get(consumer).toString());
					i++;
					continue;
				}

				fail("Unknown consumer: " + consumer);
			}

			assertEquals(3, i);
		}
	}

	@Test
	public void testIngoreInvalidConsumer() throws Exception {
		String mailboxUid = UUID.randomUUID().toString();
		Mailbox mailbox = new Mailbox();
		mailbox.name = "mailbox-" + System.nanoTime();
		mailbox.type = Type.user;
		mailbox.archived = false;
		mailbox.dataLocation = dataLocation.uid;
		mailbox.routing = Routing.internal;

		mailboxItemStore.create(Item.create(mailboxUid, null));
		Item mailboxItem = mailboxItemStore.get(mailboxUid);
		mailboxStore.create(mailboxItem, mailbox);

		DirEntry mailboxDirEntry = new DirEntry();
		mailboxDirEntry.entryUid = mailboxUid;
		mailboxDirEntry.kind = Kind.USER;
		mailboxDirEntry.path = "";
		mailboxDirEntry.displayName = mailbox.name;
		mailboxDirEntry.dataLocation = dataLocation.uid;

		Item mailboxDirEntryItem = dirItemStore.get(mailboxUid);
		dirStore.create(mailboxDirEntryItem, mailboxDirEntry);

		try (StoreClient sc = new StoreClient(cyrusIp, 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());

			System.err.println("**** Create mailbox: " + mailbox.name + "@" + domainUid);
			sc.createMailbox("user/" + mailbox.name + "@" + domainUid,
					CyrusPartition.forServerAndDomain(dataLocation, domainUid).name);
		}

		ContainerDescriptor containerDesc = ContainerDescriptor.create("mailbox:acls-" + domainUid, domainUid,
				mailboxUid, null, domainUid, true);

		List<AccessControlEntry> currentAcls = new ArrayList<>();
		currentAcls.add(AccessControlEntry.create("invalidUid", Verb.Read));

		new CyrusBackendHook().onAclChanged(null, containerDesc, Collections.<AccessControlEntry>emptyList(),
				currentAcls);

		try (StoreClient sc = new StoreClient(cyrusIp, 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());

			Map<String, Acl> acl = sc.listAcl("user/" + mailbox.name + "@" + domainUid);
			assertEquals(2, acl.size());

			int i = 0;
			for (String consumer : acl.keySet()) {
				if (consumer.equals("admin0")) {
					assertEquals(Acl.ALL.toString(), acl.get(consumer).toString());
					i++;
					continue;
				}

				if (consumer.equals(mailboxUid + "@" + domainUid)) {
					assertEquals(Acl.ALL.toString(), acl.get(consumer).toString());
					i++;
					continue;
				}

				fail("Unknown consumer: " + consumer);
			}

			assertEquals(2, i);
		}
	}

	@Test
	public void testIngoreInvalidConsumerKind() throws Exception {
		String mailboxUid = UUID.randomUUID().toString();
		Mailbox mailbox = new Mailbox();
		mailbox.name = "mailbox-" + System.nanoTime();
		mailbox.type = Type.user;
		mailbox.archived = false;
		mailbox.dataLocation = dataLocation.uid;
		mailbox.routing = Routing.internal;

		mailboxItemStore.create(Item.create(mailboxUid, null));
		Item mailboxItem = mailboxItemStore.get(mailboxUid);
		mailboxStore.create(mailboxItem, mailbox);

		DirEntry mailboxDirEntry = new DirEntry();
		mailboxDirEntry.entryUid = mailboxUid;
		mailboxDirEntry.kind = Kind.USER;
		mailboxDirEntry.path = "";
		mailboxDirEntry.displayName = mailbox.name;
		mailboxDirEntry.dataLocation = dataLocation.uid;

		Item mailboxDirEntryItem = dirItemStore.get(mailboxUid);
		dirStore.create(mailboxDirEntryItem, mailboxDirEntry);

		String mailshareUid = UUID.randomUUID().toString();
		DirEntry mailshareDirEntry = new DirEntry();
		mailshareDirEntry.entryUid = mailshareUid;
		mailshareDirEntry.kind = Kind.MAILSHARE;
		mailshareDirEntry.path = "";
		mailshareDirEntry.displayName = "mailshare";
		mailboxDirEntry.dataLocation = dataLocation.uid;

		dirItemStore.create(Item.create(mailshareUid, null));
		Item mailshareDirEntryItem = dirItemStore.get(mailshareUid);
		dirStore.create(mailshareDirEntryItem, mailshareDirEntry);

		String resourceUid = UUID.randomUUID().toString();
		DirEntry resourceDirEntry = new DirEntry();
		resourceDirEntry.entryUid = resourceUid;
		resourceDirEntry.kind = Kind.RESOURCE;
		resourceDirEntry.path = "";
		resourceDirEntry.displayName = "resource";
		resourceDirEntry.dataLocation = dataLocation.uid;

		dirItemStore.create(Item.create(resourceUid, null));
		Item resourceDirEntryItem = dirItemStore.get(resourceUid);
		dirStore.create(resourceDirEntryItem, resourceDirEntry);

		String calendarUid = UUID.randomUUID().toString();
		DirEntry calendarDirEntry = new DirEntry();
		calendarDirEntry.entryUid = calendarUid;
		calendarDirEntry.kind = Kind.CALENDAR;
		calendarDirEntry.path = "";
		calendarDirEntry.displayName = "calendar";

		dirItemStore.create(Item.create(calendarUid, null));
		Item calendarDirEntryItem = dirItemStore.get(calendarUid);
		dirStore.create(calendarDirEntryItem, calendarDirEntry);

		String abUid = UUID.randomUUID().toString();
		DirEntry adDirEntry = new DirEntry();
		adDirEntry.entryUid = abUid;
		adDirEntry.kind = Kind.ADDRESSBOOK;
		adDirEntry.path = "";
		adDirEntry.displayName = "calendar";

		dirItemStore.create(Item.create(abUid, null));
		Item adDirEntryItem = dirItemStore.get(abUid);
		dirStore.create(adDirEntryItem, adDirEntry);

		try (StoreClient sc = new StoreClient(cyrusIp, 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());

			System.err.println("**** Create mailbox: " + mailbox.name + "@" + domainUid);
			sc.createMailbox("user/" + mailbox.name + "@" + domainUid,
					CyrusPartition.forServerAndDomain(dataLocation, domainUid).name);
		}

		ContainerDescriptor containerDesc = ContainerDescriptor.create("mailbox:acls-" + domainUid, domainUid,
				mailboxUid, null, domainUid, true);

		List<AccessControlEntry> currentAcls = new ArrayList<>();
		currentAcls.add(AccessControlEntry.create(mailshareUid, Verb.Read));
		currentAcls.add(AccessControlEntry.create(resourceUid, Verb.Read));
		currentAcls.add(AccessControlEntry.create(calendarUid, Verb.Read));
		currentAcls.add(AccessControlEntry.create(abUid, Verb.Read));

		new CyrusBackendHook().onAclChanged(null, containerDesc, Collections.<AccessControlEntry>emptyList(),
				currentAcls);

		try (StoreClient sc = new StoreClient(cyrusIp, 1143, "admin0", Token.admin0())) {
			assertTrue(sc.login());

			Map<String, Acl> acl = sc.listAcl("user/" + mailbox.name + "@" + domainUid);
			assertEquals(2, acl.size());

			int i = 0;
			for (String consumer : acl.keySet()) {
				if (consumer.equals("admin0")) {
					assertEquals(Acl.ALL.toString(), acl.get(consumer).toString());
					i++;
					continue;
				}

				if (consumer.equals(mailboxUid + "@" + domainUid)) {
					assertEquals(Acl.ALL.toString(), acl.get(consumer).toString());
					i++;
					continue;
				}

				fail("Unknown consumer: " + consumer);
			}

			assertEquals(2, i);
		}
	}
}
