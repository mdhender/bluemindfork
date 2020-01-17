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
package net.bluemind.resource.lmtp.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.address.Address;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.apache.james.mime4j.message.MessageServiceFactoryImpl;
import org.apache.james.mime4j.stream.MimeConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sendmail.SendmailHelper;
import net.bluemind.core.sendmail.testhelper.FakeSendmail;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.Domain;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.Member;
import net.bluemind.group.persistence.GroupStore;
import net.bluemind.group.service.internal.ContainerGroupStoreService;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.lmtp.backend.LmtpAddress;
import net.bluemind.lmtp.backend.LmtpEnvelope;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.resource.service.internal.ResourceContainerStoreService;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.User;
import net.bluemind.user.service.internal.ContainerUserStoreService;

public class ResourceFilterTests {
	private static final String domainUid = "dom" + System.currentTimeMillis() + ".test";
	private ContainerStore containerHome;
	private User admin;
	private User user1;
	private User user2;

	private ItemValue<User> adminItem;
	private ItemValue<User> user1Item;
	private ItemValue<User> user2Item;

	private SecurityContext user1SecurityContext;
	private SecurityContext adminSecurityContext;
	private SecurityContext user2SecurityContext;
	private Container dirContainer;
	private ItemValue<Group> group1;
	private ResourceContainerStoreService resourceStore;
	private ResourceDescriptor rd;
	private String resourceUid;
	private IContainerManagement resourcesAcls;

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

		PopulateHelper.initGlobalVirt();

		containerHome = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(), SecurityContext.SYSTEM);
		ItemValue<Domain> domain = initDomain();

		resourceUid = createResource(domain);

		resourcesAcls = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainerManagement.class, ICalendarUids.TYPE + ":" + resourceUid);
		resourcesAcls.setAccessControlList(Arrays.asList(AccessControlEntry.create(user1Item.uid, Verb.Write),
				AccessControlEntry.create(group1.uid, Verb.All)));
	}

	private ItemValue<Domain> initDomain(Server... servers) throws Exception {
		ItemValue<Domain> domain = PopulateHelper.createTestDomain(domainUid, servers);

		dirContainer = containerHome.get(domainUid);

		ContainerUserStoreService userStoreService = new ContainerUserStoreService(
				new BmTestContext(SecurityContext.SYSTEM), dirContainer, domain);

		String nt = "" + System.nanoTime();
		String adm = "adm" + nt;
		adminItem = defaultUser(adm, adm);
		admin = adminItem.value;
		userStoreService.create(adminItem.uid, adm, admin);
		adminSecurityContext = new SecurityContext(adm, adm, new ArrayList<String>(),
				Arrays.asList(SecurityContext.ROLE_ADMIN), domainUid);
		Sessions.get().put(adm, adminSecurityContext);

		String u1 = "u1." + nt;
		user1Item = defaultUser(u1, u1);
		user1 = user1Item.value;
		userStoreService.create(user1Item.uid, u1, user1);
		user1SecurityContext = new SecurityContext(u1, u1, new ArrayList<String>(), new ArrayList<String>(), domainUid);
		Sessions.get().put(u1, user1SecurityContext);

		String u2 = "u2." + nt;
		user2Item = defaultUser(u2, u2);
		user2 = user2Item.value;
		userStoreService.create(user2Item.uid, u2, user2);

		user2SecurityContext = new SecurityContext(u1, u1, new ArrayList<String>(), new ArrayList<String>(), domainUid);
		Sessions.get().put(u1, user2SecurityContext);

		GroupStore groupStore = new GroupStore(JdbcTestHelper.getInstance().getDataSource(), dirContainer);
		ContainerGroupStoreService groupStoreService = new ContainerGroupStoreService(
				new BmTestContext(SecurityContext.SYSTEM), dirContainer, domain);

		String g1 = "g1." + nt;
		group1 = ItemValue.create(g1, getDefaultGroup(g1));
		groupStoreService.create(group1.uid, group1.value.name, group1.value);

		Item g1Item = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), dirContainer, SecurityContext.SYSTEM)
				.get(g1);

		Member m = new Member();
		m.type = Member.Type.user;
		m.uid = user2Item.uid;
		groupStore.addUsersMembers(g1Item,
				new ItemStore(JdbcTestHelper.getInstance().getDataSource(), dirContainer, SecurityContext.SYSTEM)
						.getMultiple(Arrays.asList(user2Item.uid)));
		return domain;
	}

	private ItemValue<User> defaultUser(String uid, String login) {
		User user = new User();
		user.login = login;
		Email em = new Email();
		em.address = login + "@" + domainUid;
		em.isDefault = true;
		em.allAliases = false;
		user.emails = Arrays.asList(em);
		user.password = "password";
		user.routing = Routing.none;
		user.dataLocation = PopulateHelper.FAKE_CYRUS_IP;

		VCard card = new VCard();
		card.identification.name = Name.create("Doe", "John", null, null, null, null);
		user.contactInfos = card;
		return ItemValue.create(uid, user);
	}

	private Group getDefaultGroup(String uid) {
		Group g = new Group();
		g.name = "group-" + System.nanoTime();
		g.description = "description " + g.name;

		g.hidden = false;
		g.hiddenMembers = false;

		Email e = new Email();
		e.address = g.name + "@" + domainUid;
		g.emails = new ArrayList<Email>(1);
		g.emails.add(e);

		return g;
	}

	private String createResource(ItemValue<Domain> domain) throws ServerFault, SQLException {
		Container resourceContainer = containerHome.get(domainUid);
		resourceStore = new ResourceContainerStoreService(new BmTestContext(SecurityContext.SYSTEM), domain,
				resourceContainer);

		String rdUid = "resource-" + System.nanoTime();
		rd = defaultDescriptor();
		resourceStore.create(rdUid, rd);

		IDirectory dir = new BmTestContext(SecurityContext.SYSTEM).provider().instance(IDirectory.class, domainUid);
		ListResult<ItemValue<DirEntry>> res = dir
				.search(DirEntryQuery.filterEmail(rd.emails.iterator().next().address));
		assertEquals(1, res.total);
		ContainerDescriptor calContainerDescriptor = ContainerDescriptor.create(ICalendarUids.TYPE + ":" + rdUid,
				"Calendar of " + rd.label, rdUid, ICalendarUids.TYPE, domainUid, true);

		IContainers containers = ServerSideServiceProvider.getProvider(adminSecurityContext)
				.instance(IContainers.class);

		containers.create(calContainerDescriptor.uid, calContainerDescriptor);

		return rdUid;
	}

	private ResourceDescriptor defaultDescriptor() {
		ResourceDescriptor rd = new ResourceDescriptor();
		rd.label = "test 1";
		rd.description = "hi !";
		rd.typeIdentifier = "testType";
		rd.dataLocation = PopulateHelper.FAKE_CYRUS_IP;
		rd.emails = Arrays.asList(Email.create("test1@" + domainUid, true));
		rd.properties = Arrays.asList(ResourceDescriptor.PropertyValue.create("test1", "value1"));
		return rd;
	}

	private InputStream open(String name) {
		return ResourceFilterTests.class.getClassLoader().getResourceAsStream("data/" + name);
	}

	private Message parseData(String name) throws Exception {
		DefaultMessageBuilder dmb = (DefaultMessageBuilder) MessageServiceFactoryImpl.newInstance().newMessageBuilder();
		MimeConfig cfg = new MimeConfig();
		cfg.setMaxHeaderLen(-1);
		cfg.setMaxHeaderCount(-1);
		cfg.setMalformedHeaderStartsBody(false);
		cfg.setMaxLineLen(-1);

		dmb.setMimeEntityConfig(cfg);

		Message m = dmb.parseMessage(open(name));
		System.out.println("" + name + " parsing done.");
		return m;
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void mailWithIMIPInfos() throws Exception {
		Message m = parseData("mailWithImip.eml");

		FakeSendmail mailer = new FakeSendmail();

		Message m2 = new ResourceFilter(mailer).filter(null, m, 0);
		assertNull(m2);

		assertFalse(mailer.mailSent);
	}

	@Test
	public void recipientNotAResource() throws Exception {
		Message m = parseData("mailWithoutImip.eml");
		m.setTo((Address) null);
		m.setTo(SendmailHelper.formatAddress(user1.login, user1.emails.iterator().next().address));

		FakeSendmail mailer = new FakeSendmail();

		LmtpEnvelope envelope = new LmtpEnvelope();

		envelope.addRecipient(
				new LmtpAddress("<" + ((Mailbox) m.getTo().iterator().next()).getAddress() + ">", null, null));

		Message m2 = new ResourceFilter(mailer).filter(envelope, m, 0);
		assertNull(m2);

		assertFalse(mailer.mailSent);
	}

	@Test
	public void resourceToRecipient() throws Exception {
		Message m = parseData("mailWithoutImip.eml");
		String origSubject = m.getSubject();
		m.setTo((Address) null);
		m.setTo(SendmailHelper.formatAddress(rd.label, rd.emails.iterator().next().address));

		FakeSendmail mailer = new FakeSendmail();

		LmtpEnvelope envelope = new LmtpEnvelope();

		envelope.addRecipient(new LmtpAddress("<" + resourceUid + "@" + domainUid + ">", null, null));

		Message m2 = new ResourceFilter(mailer).filter(envelope, m, 0);
		assertNull(m2);

		assertTrue(mailer.mailSent);

		assertEquals(1, mailer.messages.size());
		assertEquals(origSubject, mailer.messages.get(0).message.getSubject());

		assertEquals(1, mailer.messagesFrom().size());
		assertTrue(mailer.messagesFrom().contains(((Mailbox) m.getFrom().iterator().next()).getAddress()));

		assertEquals(2, mailer.messagesTo().size());
		assertTrue(mailer.messagesTo().contains(user1.emails.iterator().next().address));
		assertTrue(mailer.messagesTo().contains(user2.emails.iterator().next().address));
	}

	@Test
	public void resourceCcRecipient() throws Exception {
		Message m = parseData("mailWithoutImip.eml");
		m.setTo(SendmailHelper.formatAddress(user1.login, user1.emails.iterator().next().address));
		m.setCc((Address) null);
		m.setCc(SendmailHelper.formatAddress(rd.label, rd.emails.iterator().next().address));

		FakeSendmail mailer = new FakeSendmail();

		LmtpEnvelope envelope = new LmtpEnvelope();

		envelope.addRecipient(
				new LmtpAddress("<" + ((Mailbox) m.getTo().iterator().next()).getAddress() + ">", null, null));
		envelope.addRecipient(new LmtpAddress("<" + resourceUid + "@" + domainUid + ">", null, null));

		Message m2 = new ResourceFilter(mailer).filter(envelope, m, 0);
		assertNull(m2);

		assertTrue(mailer.mailSent);

		assertEquals(1, mailer.messages.size());

		assertEquals(1, mailer.messagesFrom().size());
		assertTrue(mailer.messagesFrom().contains(((Mailbox) m.getFrom().iterator().next()).getAddress()));

		assertEquals(2, mailer.messagesTo().size());
		assertTrue(mailer.messagesTo().contains(user1.emails.iterator().next().address));
		assertTrue(mailer.messagesTo().contains(user2.emails.iterator().next().address));
	}

	@Test
	public void resourceBccRecipient() throws Exception {
		Message m = parseData("mailWithoutImip.eml");
		m.setTo(SendmailHelper.formatAddress(user1.login, user1.emails.iterator().next().address));
		m.setBcc((Address) null);
		m.setBcc(SendmailHelper.formatAddress(rd.label, rd.emails.iterator().next().address));

		FakeSendmail mailer = new FakeSendmail();

		LmtpEnvelope envelope = new LmtpEnvelope();

		envelope.addRecipient(
				new LmtpAddress("<" + ((Mailbox) m.getTo().iterator().next()).getAddress() + ">", null, null));
		envelope.addRecipient(new LmtpAddress("<" + resourceUid + "@" + domainUid + ">", null, null));

		Message m2 = new ResourceFilter(mailer).filter(envelope, m, 0);
		assertNull(m2);

		assertTrue(mailer.mailSent);

		assertEquals(1, mailer.messages.size());

		assertEquals(1, mailer.messagesFrom().size());
		assertTrue(mailer.messagesFrom().contains(((Mailbox) m.getFrom().iterator().next()).getAddress()));

		assertEquals(2, mailer.messagesTo().size());
		assertTrue(mailer.messagesTo().contains(user1.emails.iterator().next().address));
		assertTrue(mailer.messagesTo().contains(user2.emails.iterator().next().address));
	}

	@Test
	public void resourceNoAdmins() throws Exception {
		resourcesAcls.setAccessControlList(Arrays.asList(AccessControlEntry.create(user1Item.uid, Verb.Read)));

		Message m = parseData("mailWithoutImip.eml");
		String origSubject = m.getSubject();
		m.setTo((Address) null);
		m.setTo(SendmailHelper.formatAddress(rd.label, rd.emails.iterator().next().address));

		FakeSendmail mailer = new FakeSendmail();

		LmtpEnvelope envelope = new LmtpEnvelope();

		envelope.addRecipient(new LmtpAddress("<" + resourceUid + "@" + domainUid + ">", null, null));

		Message m2 = new ResourceFilter(mailer).filter(envelope, m, 0);
		assertNull(m2);

		assertTrue(mailer.mailSent);

		assertEquals(1, mailer.messages.size());

		assertEquals("[Unable to deliver mail to resource address] " + origSubject,
				mailer.messages.get(0).message.getSubject());

		assertEquals(1, mailer.messagesFrom().size());
		assertTrue(mailer.messagesFrom().contains(((Mailbox) m.getFrom().iterator().next()).getAddress()));

		assertEquals(1, mailer.messagesTo().size());
		assertTrue(mailer.messagesFrom().contains(((Mailbox) m.getTo().iterator().next()).getAddress()));
	}
}
