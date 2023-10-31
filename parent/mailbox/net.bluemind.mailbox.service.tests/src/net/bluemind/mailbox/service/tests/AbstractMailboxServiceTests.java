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
package net.bluemind.mailbox.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;

import com.google.common.collect.Lists;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.directory.persistence.DirEntryStore;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.mailbox.persistence.MailboxStore;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.User;

public abstract class AbstractMailboxServiceTests {

	protected MailboxStore mailboxStore;
	protected ItemStore itemStore;

	protected SecurityContext defaultSecurityContext;
	protected SecurityContext userSecurityContext;
	protected Container container;

	protected Container tagContainer;
	protected String domainUid;

	protected Server smtpServer;
	protected Server pipo;
	protected Server imapServerNotAssigned;

	protected DirEntryStore dirEntryStore;

	private ItemValue<Server> dataLocation;
	protected String testUserUid;
	BmTestContext testContext;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		System.setProperty("imap.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		domainUid = "bm.lan";

		smtpServer = new Server();
		smtpServer.ip = new BmConfIni().get("smtp-role");
		smtpServer.tags = Lists.newArrayList("mail/smtp");

		pipo = new Server();
		pipo.tags = Collections.singletonList("mail/imap");
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		assertNotNull(esServer.ip);
		esServer.tags = Lists.newArrayList("bm/es");

		imapServerNotAssigned = new Server();
		imapServerNotAssigned.ip = "3.3.3.3";
		imapServerNotAssigned.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(smtpServer, pipo, imapServerNotAssigned, esServer);
		ElasticsearchTestHelper.getInstance().beforeTest();

		PopulateHelper.createTestDomain(domainUid, smtpServer, pipo, esServer);

		testUserUid = PopulateHelper.addUser("testuser" + System.currentTimeMillis(), domainUid);

		IServer serverService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				InstallationId.getIdentifier());

		dataLocation = serverService.getComplete(PopulateHelper.FAKE_CYRUS_IP);

		defaultSecurityContext = BmTestContext
				.contextWithSession("admin", "admin", domainUid, SecurityContext.ROLE_ADMIN,
						BasicRoles.ROLE_MANAGE_USER_SHARINGS, BasicRoles.ROLE_MANAGE_MAILSHARE_SHARINGS)
				.getSecurityContext();

		userSecurityContext = new SecurityContext("user", testUserUid, new ArrayList<String>(), Arrays.<String>asList(),
				domainUid);

		Sessions.get().put(userSecurityContext.getSessionId(), userSecurityContext);

		DataSource pool = JdbcTestHelper.getInstance().getDataSource();

		ContainerStore containerStore = new ContainerStore(null, pool, SecurityContext.SYSTEM);
		container = containerStore.get(domainUid);

		itemStore = new ItemStore(pool, container, defaultSecurityContext);

		mailboxStore = new MailboxStore(pool, container);

		PopulateHelper.addDomainAdmin("admin", domainUid, Routing.internal);
		PopulateHelper.domainAdmin(domainUid, defaultSecurityContext.getSubject());

		testContext = new BmTestContext(SecurityContext.SYSTEM);
		dirEntryStore = new DirEntryStore(JdbcTestHelper.getInstance().getDataSource(), container);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected abstract IMailboxes getService(SecurityContext context) throws ServerFault;

	protected Mailbox defaultMailshare(String name) {
		Mailbox mailbox = new Mailbox();
		mailbox.type = Type.mailshare;
		mailbox.routing = Routing.internal;
		mailbox.dataLocation = dataLocation.uid;

		mailbox.name = name;
		Email e = new Email();
		e.address = mailbox.name + "@bm.lan";
		e.allAliases = true;
		e.isDefault = true;
		mailbox.emails = new ArrayList<Email>(1);
		mailbox.emails.add(e);
		return mailbox;
	}

	protected Mailbox defaultMailbox(String name) {
		Mailbox mailbox = new Mailbox();
		mailbox.type = Type.user;
		mailbox.routing = Routing.internal;
		mailbox.dataLocation = dataLocation.uid;

		mailbox.name = name;
		Email e = new Email();
		e.address = mailbox.name + "@bm.lan";
		e.allAliases = true;
		e.isDefault = true;
		mailbox.emails = new ArrayList<Email>(1);
		mailbox.emails.add(e);
		return mailbox;
	}

	protected User defaultUser(String login) {
		User user = new User();
		user.login = login;
		Email em = new Email();
		em.address = login + "@" + domainUid;
		em.isDefault = true;
		em.allAliases = false;
		user.emails = Arrays.asList(em);
		user.password = "password";
		user.routing = Routing.internal;
		user.contactInfos = new VCard();
		user.contactInfos.identification.name = Name.create("Doe", "John", null, null, null, null);
		user.dataLocation = dataLocation.uid;
		return user;
	}

	protected void assertACLMatch(List<AccessControlEntry> expected, List<AccessControlEntry> actual) {
		Map<String, List<Verb>> expectedBySubject = expected.stream().collect(Collectors.groupingBy(
				AccessControlEntry::getSubject, Collectors.mapping(AccessControlEntry::getVerb, Collectors.toList())));
		Map<String, List<Verb>> actualBySubject = actual.stream().collect(Collectors.groupingBy(
				AccessControlEntry::getSubject, Collectors.mapping(AccessControlEntry::getVerb, Collectors.toList())));
		assertEquals(expectedBySubject.keySet().size(), actualBySubject.keySet().size());
		for (String subject : expectedBySubject.keySet()) {
			List<Verb> expectedVerbs = expectedBySubject.get(subject);
			List<Verb> actualVerbs = actualBySubject.get(subject);
			for (Verb verb : expectedVerbs) {
				assertTrue(actualVerbs.stream().allMatch(v -> verb.can(v)));
			}
		}

	}
}
