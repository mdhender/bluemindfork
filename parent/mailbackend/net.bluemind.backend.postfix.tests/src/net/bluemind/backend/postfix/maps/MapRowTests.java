/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.backend.postfix.maps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.backend.postfix.Activator;
import net.bluemind.backend.postfix.internal.maps.DomainInfo;
import net.bluemind.backend.postfix.internal.maps.MapRow;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.mailshare.api.IMailshare;
import net.bluemind.mailshare.api.Mailshare;
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class MapRowTests {
	private ItemValue<Domain> domain;
	private ItemValue<Server> dataLocation;
	private List<ItemValue<Server>> servers;

	@Before
	public void before() throws Exception {
		String domainUid = "bm.lan";

		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		Server imapServer = new Server();
		imapServer.ip = PopulateHelper.FAKE_CYRUS_IP;
		imapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(imapServer);

		IServer serverService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				InstallationId.getIdentifier());

		dataLocation = serverService.getComplete(imapServer.ip);
		servers = serverService.allComplete();

		domain = initTestDomain(domainUid, imapServer);

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		Activator.DISABLE_EVENT = true;
	}

	private ItemValue<Domain> initTestDomain(String domainUid, Server imapServer) throws Exception {
		return PopulateHelper.createTestDomain(domainUid, imapServer);
	}

	@Test
	public void user_archived() throws SQLException {
		User user = PopulateHelper.getUser("u1." + System.nanoTime(), domain.uid, Routing.internal);
		user.archived = true;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IUser.class, domain.uid).create(user.login, user);

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(0, mapRows.size());
	}

	@Test
	public void user_noneRoutingWithEmail() throws SQLException {
		User user = PopulateHelper.getUser("u1." + System.nanoTime(), domain.uid, Routing.none);
		user.emails = Arrays.asList(Email.create("foo@bar.fr", true));

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IUser.class, domain.uid).create(user.login, user);

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(1, mapRows.size());
		assertEquals(user.login + "@" + domain.value.name, mapRows.iterator().next().emails.iterator().next());
	}

	@Test
	public void user_noneRoutingNoEmail() throws SQLException {
		User user = PopulateHelper.getUser("u1." + System.nanoTime(), domain.uid, Routing.none);
		user.emails = null;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IUser.class, domain.uid).create(user.login, user);

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(1, mapRows.size());
		assertEquals(user.login + "@" + domain.value.name, mapRows.iterator().next().emails.iterator().next());
	}

	@Test
	public void user_externalRouting() throws SQLException {
		updateTestDomainSettings(domain.uid, DomainSettingsKeys.mail_routing_relay.name(), "relay.domain.tld");

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, ServerSideServiceProvider
				.getProvider(SecurityContext.SYSTEM).instance(IDomainSettings.class, domain.uid).get()));

		User user = PopulateHelper.getUser("u1." + System.nanoTime(), domain.uid, Routing.internal);
		user.routing = Mailbox.Routing.external;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IUser.class, domain.uid).create(user.login, user);

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(1, mapRows.size());

		MapRow mapRow = mapRows.iterator().next();
		assertEquals(Mailbox.Type.user, mapRow.type);
		assertEquals(Mailbox.Routing.external, mapRow.routing);
		assertEquals("relay.domain.tld", mapRow.dataLocation);

		assertEquals(1, mapRow.emails.size());
		assertTrue(mapRow.emails.contains(user.login + "@" + domain.value.name));

		assertEquals(user.login + "@" + domain.value.name, mapRow.getRecipients());
		assertEquals(user.login + "@" + domain.value.name, mapRow.getMailboxName());
	}

	@Test
	public void user_externalRouting_recipientIsDefault() throws SQLException {
		updateTestDomainSettings(domain.uid, DomainSettingsKeys.mail_routing_relay.name(), "relay.domain.tld");

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, ServerSideServiceProvider
				.getProvider(SecurityContext.SYSTEM).instance(IDomainSettings.class, domain.uid).get()));

		User user = PopulateHelper.getUser("u1." + System.nanoTime(), domain.uid, Routing.internal);
		user.emails = Arrays
				.asList(Email.create(user.login + "@" + domain.value.aliases.iterator().next(), true, true));
		user.routing = Mailbox.Routing.external;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IUser.class, domain.uid).create(user.login, user);

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(1, mapRows.size());

		MapRow mapRow = mapRows.iterator().next();
		assertEquals(Mailbox.Type.user, mapRow.type);
		assertEquals(Mailbox.Routing.external, mapRow.routing);
		assertEquals("relay.domain.tld", mapRow.dataLocation);

		assertEquals(2, mapRow.emails.size());
		assertTrue(mapRow.emails.contains(user.login + "@" + domain.value.name));
		assertTrue(mapRow.emails.contains(user.login + "@" + domain.value.aliases.iterator().next()));

		assertEquals(user.login + "@" + domain.value.aliases.iterator().next(), mapRow.getRecipients());
		assertEquals(user.login + "@" + domain.value.aliases.iterator().next(), mapRow.getMailboxName());
	}

	@Test
	public void user_internalRouting() throws SQLException {
		String domainAlias = domain.value.aliases.iterator().next();

		User user = PopulateHelper.getUser("u1." + System.nanoTime(), domain.uid, Routing.internal);
		user.dataLocation = dataLocation.value.address();
		user.emails = Arrays.asList(Email.create(user.login + "@" + domain.value.name, true, true),
				Email.create("alias." + user.login + "@" + domainAlias, false, false));

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IUser.class, domain.uid).create(user.login, user);

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(1, mapRows.size());

		MapRow mapRow = mapRows.iterator().next();
		assertEquals(Mailbox.Type.user, mapRow.type);
		assertEquals(Mailbox.Routing.internal, mapRow.routing);
		assertEquals(dataLocation.value.address(), mapRow.dataLocation);

		assertEquals(3, mapRow.emails.size());
		assertTrue(mapRow.emails.contains(user.login + "@" + domain.value.name));
		assertTrue(mapRow.emails.contains(user.login + "@" + domainAlias));
		assertTrue(mapRow.emails.contains("alias." + user.login + "@" + domainAlias));

		assertEquals(user.login + "@" + domain.value.name, mapRow.getRecipients());
		assertEquals(user.login + "@" + domain.value.name, mapRow.getMailboxName());
	}

	@Test
	public void user_unknowDomain() throws SQLException {
		String domainAlias = domain.value.aliases.iterator().next();

		User user = PopulateHelper.getUser("u1." + System.nanoTime(), domain.uid, Routing.internal);
		user.dataLocation = dataLocation.value.address();
		user.emails = Arrays.asList(Email.create(user.login + "@" + domain.value.name, true, true),
				Email.create("alias." + user.login + "@" + domainAlias, false, false));

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IUser.class, domain.uid).create(user.login, user);

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(0, mapRows.size());
	}

	@Test
	public void user_nullDatalocation() throws SQLException {
		String domainAlias = domain.value.aliases.iterator().next();

		User user = PopulateHelper.getUser("u1." + System.nanoTime(), domain.uid, Routing.internal);
		user.dataLocation = dataLocation.value.address();
		user.emails = Arrays.asList(Email.create(user.login + "@" + domain.value.name, true, true),
				Email.create("alias." + user.login + "@" + domainAlias, false, false));

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IUser.class, domain.uid).create(user.login, user);

		Connection conn = JdbcActivator.getInstance().getDataSource().getConnection();
		PreparedStatement st = null;
		try {
			st = conn.prepareStatement("UPDATE t_directory_entry SET datalocation=NULL");
			st.executeUpdate();
		} finally {
			JdbcHelper.cleanup(conn, null, st);
		}

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(0, mapRows.size());
	}

	@Test
	public void mailshare_internalRouting_noEmail() throws SQLException {
		Mailshare mailshare = new Mailshare();
		mailshare.archived = false;
		mailshare.routing = Routing.internal;
		mailshare.name = "m1." + System.nanoTime();

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IMailshare.class, domain.uid)
				.create(mailshare.name, mailshare);

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(0, mapRows.size());
	}

	@Test
	public void mailshare_internalRouting() throws SQLException {
		String domainAlias = domain.value.aliases.iterator().next();

		Mailshare mailshare = new Mailshare();
		mailshare.archived = false;
		mailshare.routing = Routing.internal;
		mailshare.name = "m1." + System.nanoTime();
		mailshare.emails = Arrays.asList(Email.create(mailshare.name + "@" + domain.value.name, true, false),
				Email.create("alias." + mailshare.name + "@" + domain.value.name, false, true));

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IMailshare.class, domain.uid)
				.create(mailshare.name, mailshare);

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(1, mapRows.size());

		MapRow mapRow = mapRows.iterator().next();
		assertEquals(Mailbox.Type.mailshare, mapRow.type);
		assertEquals(Mailbox.Routing.internal, mapRow.routing);
		assertEquals(dataLocation.value.address(), mapRow.dataLocation);

		assertEquals(3, mapRow.emails.size());
		assertTrue(mapRow.emails.contains(mailshare.name + "@" + domain.value.name));
		assertTrue(mapRow.emails.contains("alias." + mailshare.name + "@" + domainAlias));
		assertTrue(mapRow.emails.contains("alias." + mailshare.name + "@" + domainAlias));

		assertEquals("+" + mailshare.name + "@" + domain.value.name, mapRow.getRecipients());
		assertEquals("+" + mailshare.name + "@" + domain.value.name, mapRow.getMailboxName());
	}

	@Test
	public void mailshare_internalRouting_archived() throws SQLException {
		Mailshare mailshare = new Mailshare();
		mailshare.archived = true;
		mailshare.routing = Routing.internal;
		mailshare.name = "m1." + System.nanoTime();
		mailshare.emails = Arrays.asList(Email.create(mailshare.name + "@" + domain.value.name, true, false),
				Email.create("alias." + mailshare.name + "@" + domain.value.name, false, true));

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IMailshare.class, domain.uid)
				.create(mailshare.name, mailshare);

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(0, mapRows.size());
	}

	@Test
	public void mailshare_externalRouting() throws SQLException {
		updateTestDomainSettings(domain.uid, DomainSettingsKeys.mail_routing_relay.name(), "relay.domain.tld");

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, ServerSideServiceProvider
				.getProvider(SecurityContext.SYSTEM).instance(IDomainSettings.class, domain.uid).get()));

		Mailshare mailshare = new Mailshare();
		mailshare.archived = false;
		mailshare.routing = Routing.external;
		mailshare.name = "m1." + System.nanoTime();
		mailshare.emails = Arrays.asList(Email.create(mailshare.name + "@" + domain.value.name, true, false));

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IMailshare.class, domain.uid)
				.create(mailshare.name, mailshare);

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(1, mapRows.size());

		MapRow mapRow = mapRows.iterator().next();
		assertEquals(Mailbox.Type.mailshare, mapRow.type);
		assertEquals(Mailbox.Routing.external, mapRow.routing);
		assertEquals("relay.domain.tld", mapRow.dataLocation);

		assertEquals(1, mapRow.emails.size());
		assertTrue(mapRow.emails.contains(mailshare.name + "@" + domain.value.name));

		assertEquals(mailshare.name + "@" + domain.value.name, mapRow.getRecipients());
		assertEquals(mailshare.name + "@" + domain.value.name, mapRow.getMailboxName());
	}

	@Test
	public void mailshare_externalRouting_recipientIsDefaultAddress() throws SQLException {
		updateTestDomainSettings(domain.uid, DomainSettingsKeys.mail_routing_relay.name(), "relay.domain.tld");

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, ServerSideServiceProvider
				.getProvider(SecurityContext.SYSTEM).instance(IDomainSettings.class, domain.uid).get()));

		Mailshare mailshare = new Mailshare();
		mailshare.archived = false;
		mailshare.routing = Routing.external;
		mailshare.name = "m1." + System.nanoTime();
		mailshare.emails = Arrays
				.asList(Email.create(mailshare.name + "@" + domain.value.aliases.iterator().next(), true, true));

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IMailshare.class, domain.uid)
				.create(mailshare.name, mailshare);

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(1, mapRows.size());

		MapRow mapRow = mapRows.iterator().next();
		assertEquals(Mailbox.Type.mailshare, mapRow.type);
		assertEquals(Mailbox.Routing.external, mapRow.routing);
		assertEquals("relay.domain.tld", mapRow.dataLocation);

		assertEquals(2, mapRow.emails.size());
		assertTrue(mapRow.emails.contains(mailshare.name + "@" + domain.value.name));
		assertTrue(mapRow.emails.contains(mailshare.name + "@" + domain.value.aliases.iterator().next()));

		assertEquals(mailshare.name + "@" + domain.value.aliases.iterator().next(), mapRow.getRecipients());
		assertEquals(mailshare.name + "@" + domain.value.aliases.iterator().next(), mapRow.getMailboxName());
	}

	@Test
	public void resource_internal() throws SQLException {
		ResourceDescriptor resource = new ResourceDescriptor();
		resource.label = "r1." + System.nanoTime();
		resource.typeIdentifier = "default";
		resource.emails = Arrays.asList(Email.create(resource.label + "@" + domain.value.name, true, false));

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IResources.class, domain.uid)
				.create(resource.label, resource);

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(1, mapRows.size());

		MapRow mapRow = mapRows.iterator().next();
		assertEquals(Mailbox.Type.resource, mapRow.type);
		assertEquals(Mailbox.Routing.internal, mapRow.routing);
		assertEquals(dataLocation.value.address(), mapRow.dataLocation);

		assertEquals(1, mapRow.emails.size());
		assertTrue(mapRow.emails.contains(resource.label + "@" + domain.value.name));

		assertEquals("+" + resource.label + "@" + domain.value.name, mapRow.getRecipients());
		assertEquals("+" + resource.label + "@" + domain.value.name, mapRow.getMailboxName());
	}

	@Test
	public void group_noMember_noEmail_noMailArchive() throws SQLException {
		Group group = new Group();
		group.name = "g1." + System.nanoTime();
		group.mailArchived = false;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).create(group.name,
				group);

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(0, mapRows.size());
	}

	@Test
	public void group_noMember_withEmail_noMailArchive() throws SQLException {
		Group group = new Group();
		group.name = "g1." + System.nanoTime();
		group.emails = Arrays.asList(Email.create(group.name + "@" + domain.value.name, true, false));
		group.mailArchived = false;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).create(group.name,
				group);

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(0, mapRows.size());
	}

	@Test
	public void group_noMember_noEmail_withMailArchive() throws SQLException {
		Group group = new Group();
		group.name = "g1." + System.nanoTime();
		group.mailArchived = true;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).create(group.name,
				group);

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(0, mapRows.size());
	}

	@Test
	public void group_noMember_withEmail_withMailArchive() throws SQLException {
		Group group = new Group();
		group.name = "g1." + System.nanoTime();
		group.emails = Arrays.asList(Email.create(group.name + "@" + domain.value.name, true, false));
		group.mailArchived = true;
		group.dataLocation = dataLocation.value.address();

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).create(group.name,
				group);

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(1, mapRows.size());

		MapRow mapRow = mapRows.iterator().next();
		assertEquals(Mailbox.Type.group, mapRow.type);
		assertEquals(Mailbox.Routing.internal, mapRow.routing);
		assertEquals(dataLocation.value.address(), mapRow.dataLocation);

		assertEquals(1, mapRow.emails.size());
		assertTrue(mapRow.emails.contains(group.name + "@" + domain.value.name));

		assertEquals("+_" + group.name + "@" + domain.value.name, mapRow.getRecipients());
		assertEquals("+_" + group.name + "@" + domain.value.name, mapRow.getMailboxName());
	}

	@Test
	public void group_withUserMemberNoneRouting_noEmail_noMailArchive() throws SQLException {
		User user = PopulateHelper.getUser("u1." + System.nanoTime(), domain.uid, Routing.none);

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IUser.class, domain.uid).create(user.login, user);

		Group group = new Group();
		group.name = "g1." + System.nanoTime();
		group.mailArchived = false;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).create(group.name,
				group);

		Member member = new Member();
		member.type = Member.Type.user;
		member.uid = user.login;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).add(group.name,
				Arrays.asList(member));

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(1, mapRows.size());
		assertEquals(user.login + "@" + domain.value.name, mapRows.iterator().next().emails.iterator().next());
	}

	@Test
	public void group_withUserMemberNoneRoutingNoEmail_withEmail_noMailArchive() throws SQLException {
		User user = PopulateHelper.getUser("u1." + System.nanoTime(), domain.uid, Routing.none);
		user.emails = null;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IUser.class, domain.uid).create(user.login, user);

		Group group = new Group();
		group.name = "g1." + System.nanoTime();
		group.emails = Arrays.asList(Email.create(group.name + "@" + domain.value.name, true, false));
		group.mailArchived = false;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).create(group.name,
				group);

		Member member = new Member();
		member.type = Member.Type.user;
		member.uid = user.login;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).add(group.name,
				Arrays.asList(member));

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(2, mapRows.size());
	}

	@Test
	public void group_withUserMemberNoneRouting_withEmail_noMailArchive() throws SQLException {
		User user = PopulateHelper.getUser("u1." + System.nanoTime(), domain.uid, Routing.none);
		user.emails = Arrays.asList(Email.create("foo@bar.tld", true));

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IUser.class, domain.uid).create(user.login, user);

		Group group = new Group();
		group.name = "g1." + System.nanoTime();
		group.emails = Arrays.asList(Email.create(group.name + "@" + domain.value.name, true, false));
		group.mailArchived = false;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).create(group.name,
				group);

		Member member = new Member();
		member.type = Member.Type.user;
		member.uid = user.login;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).add(group.name,
				Arrays.asList(member));

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		Iterator<MapRow> iterator = mapRows.iterator();
		MapRow mapRow = iterator.next();
		assertEquals(Mailbox.Type.user, mapRow.type);
		assertEquals(Mailbox.Routing.internal, mapRow.routing);
		assertEquals(user.login + "@" + domain.value.name, mapRow.getMailboxName());
		assertEquals(user.login + "@" + domain.value.name, mapRow.getRecipients());
		assertFalse(mapRow.emails.contains("foo@bar.tld"));
		assertTrue(mapRow.emails.contains(user.login + "@" + domain.value.name));
		assertNotNull(mapRow.dataLocation);
	}

	@Test
	public void group_withUserMemberNoneRouting_noEmail_withMailArchive() throws SQLException {
		User user = PopulateHelper.getUser("u1." + System.nanoTime(), domain.uid, Routing.none);

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IUser.class, domain.uid).create(user.login, user);

		Group group = new Group();
		group.name = "g1." + System.nanoTime();
		group.mailArchived = true;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).create(group.name,
				group);

		Member member = new Member();
		member.type = Member.Type.user;
		member.uid = user.login;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).add(group.name,
				Arrays.asList(member));

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(1, mapRows.size());
		assertEquals(user.login + "@" + domain.value.name, mapRows.iterator().next().emails.iterator().next());
	}

	@Test
	public void group_withUserMemberNoneRoutingNoEmail_withEmail_withMailArchive() throws SQLException {
		User user = PopulateHelper.getUser("u1." + System.nanoTime(), domain.uid, Routing.none);
		user.emails = null;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IUser.class, domain.uid).create(user.login, user);

		Group group = new Group();
		group.name = "g1." + System.nanoTime();
		group.emails = Arrays.asList(Email.create(group.name + "@" + domain.value.name, true, false));
		group.mailArchived = true;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).create(group.name,
				group);

		Member member = new Member();
		member.type = Member.Type.user;
		member.uid = user.login;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).add(group.name,
				Arrays.asList(member));

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(2, mapRows.size());

		Iterator<MapRow> iterator = mapRows.iterator();
		MapRow mapRow;

		mapRow = iterator.next();
		assertEquals(Mailbox.Type.user, mapRow.type);

		mapRow = iterator.next();
		assertEquals(Mailbox.Type.group, mapRow.type);
		assertEquals(Mailbox.Routing.internal, mapRow.routing);
		assertEquals(dataLocation.value.address(), mapRow.dataLocation);
		assertEquals(1, mapRow.emails.size());
		assertTrue(mapRow.emails.contains(group.name + "@" + domain.value.name));

		assertTrue(mapRow.getRecipients().contains("+_" + group.name + "@" + domain.value.name));
		assertTrue(mapRow.getRecipients().contains(user.login + "@" + domain.value.name));
		assertEquals("+_" + group.name + "@" + domain.value.name, mapRow.getMailboxName());

	}

	@Test
	public void group_withUserMemberNoneRouting_withEmail_withMailArchive() throws SQLException {
		User user = PopulateHelper.getUser("u1." + System.nanoTime(), domain.uid, Routing.none);
		user.emails = Arrays.asList(Email.create("foo@bar.tld", true));

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IUser.class, domain.uid).create(user.login, user);

		Group group = new Group();
		group.name = "g1." + System.nanoTime();
		group.emails = Arrays.asList(Email.create(group.name + "@" + domain.value.name, true, false));
		group.mailArchived = true;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).create(group.name,
				group);

		Member member = new Member();
		member.type = Member.Type.user;
		member.uid = user.login;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).add(group.name,
				Arrays.asList(member));

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(2, mapRows.size());
		for (MapRow mr : mapRows) {
			System.err.println("mr: " + mr);
		}

		Iterator<MapRow> iterator = mapRows.iterator();
		MapRow mapRow;

		mapRow = iterator.next();
		assertEquals(Mailbox.Type.user, mapRow.type);
		assertEquals(Mailbox.Routing.internal, mapRow.routing);
		assertEquals(1, mapRow.emails.size());
		assertTrue(mapRow.emails.contains(user.login + "@" + domain.value.name));

		mapRow = iterator.next();
		assertEquals(Mailbox.Type.group, mapRow.type);
		assertEquals(Mailbox.Routing.internal, mapRow.routing);
		assertEquals(dataLocation.value.address(), mapRow.dataLocation);

		assertEquals(1, mapRow.emails.size());
		assertTrue(mapRow.emails.contains(group.name + "@" + domain.value.name));

		List<String> recipients = Arrays.asList(mapRow.getRecipients().split(","));
		assertEquals(2, recipients.size());
		assertTrue(recipients.contains("+_" + group.name + "@" + domain.value.name));
		assertTrue(recipients.contains(user.login + "@" + domain.value.name));
		assertEquals("+_" + group.name + "@" + domain.value.name, mapRow.getMailboxName());

	}

	@Test
	public void group_withUserMemberInternalRouting_withEmail_withMailArchive() throws SQLException {
		User user = PopulateHelper.getUser("u1." + System.nanoTime(), domain.uid, Routing.internal);
		user.routing = Mailbox.Routing.internal;
		user.dataLocation = dataLocation.value.address();

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IUser.class, domain.uid).create(user.login, user);

		Group group = new Group();
		group.name = "g1." + System.nanoTime();
		group.emails = Arrays.asList(Email.create(group.name + "@" + domain.value.name, true, true));
		group.mailArchived = true;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).create(group.name,
				group);

		Member member = new Member();
		member.type = Member.Type.user;
		member.uid = user.login;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).add(group.name,
				Arrays.asList(member));

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(2, mapRows.size());

		for (MapRow mapRow : mapRows) {
			if (mapRow.type == Type.user) {
				continue;
			}

			assertEquals(Mailbox.Type.group, mapRow.type);
			assertEquals(Mailbox.Routing.internal, mapRow.routing);
			assertEquals(dataLocation.value.address(), mapRow.dataLocation);

			assertEquals(2, mapRow.emails.size());
			assertTrue(mapRow.emails.contains(group.name + "@" + domain.value.name));
			assertTrue(mapRow.emails.contains(group.name + "@" + domain.value.aliases.iterator().next()));

			assertEquals("+_" + group.name + "@" + domain.value.name, mapRow.getMailboxName());

			assertNotNull(mapRow.getRecipients());
			List<String> parts = Arrays.asList(mapRow.getRecipients().split(","));
			assertEquals(2, parts.size());
			assertTrue(parts.contains("+_" + group.name + "@" + domain.value.name));
			assertTrue(parts.contains(user.login + "@" + domain.value.name));
		}
	}

	@Test
	public void group_withUserMemberExternalRouting_withEmail_withMailArchive() throws SQLException {
		updateTestDomainSettings(domain.uid, DomainSettingsKeys.mail_routing_relay.name(), "relay.domain.tld");

		Map<String, Map<String, String>> domainSettings = new HashMap<>();
		domainSettings.put(domain.uid, ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domain.uid).get());

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, ServerSideServiceProvider
				.getProvider(SecurityContext.SYSTEM).instance(IDomainSettings.class, domain.uid).get()));

		User user = PopulateHelper.getUser("u1." + System.nanoTime(), domain.uid, Routing.internal);
		user.routing = Mailbox.Routing.external;
		user.dataLocation = dataLocation.value.address();

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IUser.class, domain.uid).create(user.login, user);

		Group group = new Group();
		group.name = "g1." + System.nanoTime();
		group.emails = Arrays.asList(Email.create(group.name + "@" + domain.value.name, true, true));
		group.mailArchived = true;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).create(group.name,
				group);

		Member member = new Member();
		member.type = Member.Type.user;
		member.uid = user.login;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).add(group.name,
				Arrays.asList(member));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(2, mapRows.size());

		for (MapRow mapRow : mapRows) {
			if (mapRow.type == Type.user) {
				continue;
			}

			assertEquals(Mailbox.Type.group, mapRow.type);
			assertEquals(Mailbox.Routing.internal, mapRow.routing);
			assertEquals(dataLocation.value.address(), mapRow.dataLocation);

			assertEquals(2, mapRow.emails.size());
			assertTrue(mapRow.emails.contains(group.name + "@" + domain.value.name));
			assertTrue(mapRow.emails.contains(group.name + "@" + domain.value.aliases.iterator().next()));

			assertEquals("+_" + group.name + "@" + domain.value.name, mapRow.getMailboxName());

			assertNotNull(mapRow.getRecipients());
			List<String> parts = Arrays.asList(mapRow.getRecipients().split(","));
			assertEquals(2, parts.size());
			assertTrue(parts.contains("+_" + group.name + "@" + domain.value.name));
			assertTrue(parts.contains(user.login + "@" + domain.value.name));
		}
	}

	@Test
	public void group_withUserMemberInternalRouting_withEmail_noMailArchive() throws SQLException {
		User user = PopulateHelper.getUser("u1." + System.nanoTime(), domain.uid, Routing.internal);
		user.dataLocation = dataLocation.value.address();

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IUser.class, domain.uid).create(user.login, user);

		Group group = new Group();
		group.name = "g1." + System.nanoTime();
		group.emails = Arrays.asList(Email.create(group.name + "@" + domain.value.name, true, true));
		group.mailArchived = false;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).create(group.name,
				group);

		Member member = new Member();
		member.type = Member.Type.user;
		member.uid = user.login;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).add(group.name,
				Arrays.asList(member));

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(2, mapRows.size());

		for (MapRow mapRow : mapRows) {
			if (mapRow.type == Type.user) {
				continue;
			}

			assertEquals(Mailbox.Type.group, mapRow.type);
			assertEquals(Mailbox.Routing.none, mapRow.routing);
			assertNull(mapRow.dataLocation);

			assertEquals(2, mapRow.emails.size());
			assertTrue(mapRow.emails.contains(group.name + "@" + domain.value.name));
			assertTrue(mapRow.emails.contains(group.name + "@" + domain.value.aliases.iterator().next()));

			assertNull(mapRow.getMailboxName());

			assertEquals(user.login + "@" + domain.value.name, mapRow.getRecipients());
		}
	}

	@Test
	public void group_withUserMemberInternalRouting_noEmail_withMailArchive() throws SQLException {
		User user = PopulateHelper.getUser("u1." + System.nanoTime(), domain.uid, Routing.internal);
		user.dataLocation = dataLocation.value.address();

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IUser.class, domain.uid).create(user.login, user);

		Group group = new Group();
		group.name = "g1." + System.nanoTime();
		group.mailArchived = true;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).create(group.name,
				group);

		Member member = new Member();
		member.type = Member.Type.user;
		member.uid = user.login;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).add(group.name,
				Arrays.asList(member));

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(1, mapRows.size());
		assertEquals(Type.user, mapRows.iterator().next().type);
	}

	@Test
	public void group_withUserMemberInternalRouting_noEmail_noMailArchive() throws SQLException {
		User user = PopulateHelper.getUser("u1." + System.nanoTime(), domain.uid, Routing.internal);
		user.dataLocation = dataLocation.value.address();

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IUser.class, domain.uid).create(user.login, user);

		Group group = new Group();
		group.name = "g1." + System.nanoTime();
		group.mailArchived = false;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).create(group.name,
				group);

		Member member = new Member();
		member.type = Member.Type.user;
		member.uid = user.login;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).add(group.name,
				Arrays.asList(member));

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(1, mapRows.size());
		assertEquals(Type.user, mapRows.iterator().next().type);
	}

	@Test
	public void group_withGroupMember_noEmail_noChildEmail_noMailArchive_noChildMailArchive() throws SQLException {
		Group g1 = new Group();
		g1.name = "g1." + System.nanoTime();
		g1.mailArchived = false;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).create(g1.name, g1);

		Group g2 = new Group();
		g2.name = "g1." + System.nanoTime();
		g2.mailArchived = false;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).create(g2.name, g2);

		Member member = new Member();
		member.type = Member.Type.group;
		member.uid = g2.name;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).add(g1.name,
				Arrays.asList(member));

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(0, mapRows.size());
	}

	@Test
	public void group_withGroupMember_withEmail_noChildEmail_noMailArchive_noChildMailArchive() throws SQLException {
		Group g1 = new Group();
		g1.name = "g1." + System.nanoTime();
		g1.emails = Arrays.asList(Email.create(g1.name + "@" + domain.value.name, true, false));
		g1.mailArchived = false;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).create(g1.name, g1);

		Group g2 = new Group();
		g2.name = "g1." + System.nanoTime();
		g2.mailArchived = false;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).create(g2.name, g2);

		Member member = new Member();
		member.type = Member.Type.group;
		member.uid = g2.name;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).add(g1.name,
				Arrays.asList(member));

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(0, mapRows.size());
	}

	@Test
	public void group_withGroupMember_withEmail_noChildEmail_withMailArchive_noChildMailArchive() throws SQLException {
		Group g1 = new Group();
		g1.name = "g1." + System.nanoTime();
		g1.emails = Arrays.asList(Email.create(g1.name + "@" + domain.value.name, true, false));
		g1.mailArchived = true;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).create(g1.name, g1);

		Group g2 = new Group();
		g2.name = "g1." + System.nanoTime();
		g2.mailArchived = false;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).create(g2.name, g2);

		Member member = new Member();
		member.type = Member.Type.group;
		member.uid = g2.name;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).add(g1.name,
				Arrays.asList(member));

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(1, mapRows.size());

		MapRow mapRow = mapRows.iterator().next();
		assertEquals(Mailbox.Type.group, mapRow.type);
		assertEquals(Mailbox.Routing.internal, mapRow.routing);
		assertEquals(dataLocation.value.address(), mapRow.dataLocation);

		assertEquals(1, mapRow.emails.size());
		assertTrue(mapRow.emails.contains(g1.name + "@" + domain.value.name));

		assertEquals("+_" + g1.name + "@" + domain.value.name, mapRow.getMailboxName());
		assertEquals("+_" + g1.name + "@" + domain.value.name, mapRow.getRecipients());
	}

	@Test
	public void group_withGroupMember_withEmail_withChildEmail_withMailArchive_noChildMailArchive()
			throws SQLException {
		Group g1 = new Group();
		g1.name = "g1." + System.nanoTime();
		g1.emails = Arrays.asList(Email.create(g1.name + "@" + domain.value.name, true, false));
		g1.mailArchived = true;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).create(g1.name, g1);

		Group g2 = new Group();
		g2.name = "g1." + System.nanoTime();
		g2.emails = Arrays.asList(Email.create(g2.name + "@" + domain.value.name, true, false));
		g2.mailArchived = false;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).create(g2.name, g2);

		Member member = new Member();
		member.type = Member.Type.group;
		member.uid = g2.name;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).add(g1.name,
				Arrays.asList(member));

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(1, mapRows.size());

		MapRow mapRow = mapRows.iterator().next();
		assertEquals(Mailbox.Type.group, mapRow.type);
		assertEquals(Mailbox.Routing.internal, mapRow.routing);
		assertEquals(dataLocation.value.address(), mapRow.dataLocation);

		assertEquals(1, mapRow.emails.size());
		assertTrue(mapRow.emails.contains(g1.name + "@" + domain.value.name));

		assertEquals("+_" + g1.name + "@" + domain.value.name, mapRow.getMailboxName());
		assertEquals("+_" + g1.name + "@" + domain.value.name, mapRow.getRecipients());
	}

	@Test
	public void group_withGroupMember_withEmail_withChildEmail_withMailArchive_withChildMailArchive()
			throws SQLException {
		Group g1 = new Group();
		g1.name = "g1." + System.nanoTime();
		g1.emails = Arrays.asList(Email.create(g1.name + "@" + domain.value.name, true, false));
		g1.mailArchived = true;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).create(g1.name, g1);

		Group g2 = new Group();
		g2.name = "g1." + System.nanoTime();
		g2.emails = Arrays.asList(Email.create(g2.name + "@" + domain.value.name, true, false));
		g2.mailArchived = true;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).create(g2.name, g2);

		Member member = new Member();
		member.type = Member.Type.group;
		member.uid = g2.name;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).add(g1.name,
				Arrays.asList(member));

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(2, mapRows.size());

		boolean g1Found = false;
		boolean g2Found = false;
		for (MapRow mapRow : mapRows) {
			assertEquals(Mailbox.Type.group, mapRow.type);
			assertEquals(Mailbox.Routing.internal, mapRow.routing);
			assertEquals(dataLocation.value.address(), mapRow.dataLocation);

			assertEquals(1, mapRow.emails.size());
			if (mapRow.emails.contains(g1.name + "@" + domain.value.name)) {
				g1Found = true;

				assertEquals("+_" + g1.name + "@" + domain.value.name, mapRow.getMailboxName());

				List<String> parts = Arrays.asList(mapRow.getRecipients().split(","));
				assertEquals(2, parts.size());
				assertTrue(parts.contains("+_" + g1.name + "@" + domain.value.name));
				assertTrue(parts.contains(g2.name + "@" + domain.value.name));
			} else if (mapRow.emails.contains(g2.name + "@" + domain.value.name)) {
				g2Found = true;

				assertEquals("+_" + g2.name + "@" + domain.value.name, mapRow.getMailboxName());
				assertEquals("+_" + g2.name + "@" + domain.value.name, mapRow.getRecipients());
			} else {
				fail("Unknow email: " + mapRow.emails.iterator().next());
			}
		}

		assertTrue(g1Found && g2Found);
	}

	@Test
	public void group_withGroupMember_withUserMember_withChildUserMember() throws SQLException {
		Group g1 = new Group();
		g1.name = "g1." + System.nanoTime();
		g1.emails = Arrays.asList(Email.create(g1.name + "@" + domain.value.name, true, false));
		g1.mailArchived = false;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).create(g1.name, g1);

		Group g2 = new Group();
		g2.name = "g1." + System.nanoTime();
		g2.emails = Arrays.asList(Email.create(g2.name + "@" + domain.value.name, true, false));
		g2.mailArchived = false;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).create(g2.name, g2);

		Member member = new Member();
		member.type = Member.Type.group;
		member.uid = g2.name;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).add(g1.name,
				Arrays.asList(member));

		User u1 = PopulateHelper.getUser("u1." + System.nanoTime(), domain.uid, Routing.internal);
		u1.routing = Mailbox.Routing.internal;
		u1.dataLocation = dataLocation.value.address();

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IUser.class, domain.uid).create(u1.login, u1);

		member = new Member();
		member.type = Member.Type.user;
		member.uid = u1.login;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).add(g1.name,
				Arrays.asList(member));

		User u2 = PopulateHelper.getUser("u2." + System.nanoTime(), domain.uid, Routing.internal);
		u2.routing = Mailbox.Routing.internal;
		u2.dataLocation = dataLocation.value.address();

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IUser.class, domain.uid).create(u2.login, u2);

		member = new Member();
		member.type = Member.Type.user;
		member.uid = u2.login;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).add(g2.name,
				Arrays.asList(member));

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(4, mapRows.size());

		boolean g1Found = false;
		boolean g2Found = false;
		boolean u1Found = false;
		boolean u2Found = false;
		for (MapRow mapRow : mapRows) {
			if (mapRow.emails.contains(u1.login + "@" + domain.value.name)) {
				u1Found = true;

				assertEquals(Mailbox.Type.user, mapRow.type);
				assertEquals(Mailbox.Routing.internal, mapRow.routing);
				assertEquals(dataLocation.value.address(), mapRow.dataLocation);

				assertEquals(1, mapRow.emails.size());

				assertEquals(u1.login + "@" + domain.value.name, mapRow.getMailboxName());
				assertEquals(u1.login + "@" + domain.value.name, mapRow.getRecipients());
			} else if (mapRow.emails.contains(u2.login + "@" + domain.value.name)) {
				u2Found = true;

				assertEquals(Mailbox.Type.user, mapRow.type);
				assertEquals(Mailbox.Routing.internal, mapRow.routing);
				assertEquals(dataLocation.value.address(), mapRow.dataLocation);

				assertEquals(1, mapRow.emails.size());

				assertEquals(u2.login + "@" + domain.value.name, mapRow.getMailboxName());
				assertEquals(u2.login + "@" + domain.value.name, mapRow.getRecipients());
			} else if (mapRow.emails.contains(g1.name + "@" + domain.value.name)) {
				g1Found = true;

				assertEquals(Mailbox.Type.group, mapRow.type);
				assertEquals(Mailbox.Routing.none, mapRow.routing);
				assertNull(mapRow.dataLocation);

				assertEquals(1, mapRow.emails.size());

				assertNull(mapRow.getMailboxName());

				List<String> parts = Arrays.asList(mapRow.getRecipients().split(","));
				assertEquals(2, parts.size());
				assertTrue(parts.contains(g2.name + "@" + domain.value.name));
				assertTrue(parts.contains(u1.login + "@" + domain.value.name));
			} else if (mapRow.emails.contains(g2.name + "@" + domain.value.name)) {
				g2Found = true;

				assertEquals(Mailbox.Type.group, mapRow.type);
				assertEquals(Mailbox.Routing.none, mapRow.routing);
				assertNull(mapRow.dataLocation);

				assertEquals(1, mapRow.emails.size());

				assertNull(mapRow.getMailboxName());

				assertEquals(u2.login + "@" + domain.value.name, mapRow.getRecipients());
			} else {
				fail("Unknow email: " + mapRow.emails.iterator().next());
			}
		}

		assertTrue(g1Found && g2Found && u1Found && u2Found);
	}

	@Test
	public void group_withExternalUserMember_withEmail_withMailArchive() throws SQLException {
		String extUserUid = PopulateHelper.addExternalUser(domain.uid, "ext@user.com", "ExtUser Name");

		Group group = new Group();
		group.name = "g1." + System.nanoTime();
		group.emails = Arrays.asList(Email.create(group.name + "@" + domain.value.name, true, false));
		group.mailArchived = true;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).create(group.name,
				group);

		Member member = new Member();
		member.type = Member.Type.external_user;
		member.uid = extUserUid;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).add(group.name,
				Arrays.asList(member));

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(1, mapRows.size());

		MapRow mapRow = mapRows.iterator().next();
		assertEquals(Mailbox.Type.group, mapRow.type);
		assertEquals(Mailbox.Routing.internal, mapRow.routing);
		assertEquals(dataLocation.value.address(), mapRow.dataLocation);

		assertEquals(1, mapRow.emails.size());
		assertTrue(mapRow.emails.contains(group.name + "@" + domain.value.name));

		assertEquals("+_" + group.name + "@" + domain.value.name, mapRow.getMailboxName());

		String[] recipients = mapRow.getRecipients().split(",");
		assertEquals(2, recipients.length);
		for (String recipient : recipients) {
			if (!recipient.equals("+_" + group.name + "@" + domain.value.name) && !recipient.equals("ext@user.com")) {
				fail("Unknow recipient: " + recipient);
			}
		}
	}

	@Test
	public void group_withExternalUserMember_withEmail_noMailArchive() throws SQLException {
		String extUserUid = PopulateHelper.addExternalUser(domain.uid, "ext@user.com", "ExtUser Name");

		Group group = new Group();
		group.name = "g1." + System.nanoTime();
		group.emails = Arrays.asList(Email.create(group.name + "@" + domain.value.name, true, false));
		group.mailArchived = false;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).create(group.name,
				group);

		Member member = new Member();
		member.type = Member.Type.external_user;
		member.uid = extUserUid;

		new BmTestContext(SecurityContext.SYSTEM).provider().instance(IGroup.class, domain.uid).add(group.name,
				Arrays.asList(member));

		Map<String, DomainInfo> domainInfoByUid = new HashMap<>();
		domainInfoByUid.put(domain.uid, DomainInfo.build(domain, Collections.emptyMap()));

		Collection<MapRow> mapRows = MapRow.build(new BmTestContext(SecurityContext.SYSTEM), servers, domainInfoByUid);
		assertNotNull(mapRows);
		assertEquals(1, mapRows.size());

		MapRow mapRow = mapRows.iterator().next();
		assertEquals(Mailbox.Type.group, mapRow.type);
		assertEquals(Mailbox.Routing.none, mapRow.routing);
		assertNull(mapRow.dataLocation);

		assertEquals(1, mapRow.emails.size());
		assertTrue(mapRow.emails.contains(group.name + "@" + domain.value.name));

		assertNull(mapRow.getMailboxName());
		assertEquals("ext@user.com", mapRow.getRecipients());
	}

	private void updateTestDomainSettings(String domainUid, String key, String value) throws ServerFault {
		IDomainSettings service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);
		Map<String, String> settings = service.get();
		settings.put(key, value);
		service.set(settings);
	}
}