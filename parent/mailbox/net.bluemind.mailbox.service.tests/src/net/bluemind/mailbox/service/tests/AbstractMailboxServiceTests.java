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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.collect.Lists;

import net.bluemind.backend.cyrus.CyrusAdmins;
import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.container.persistance.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.directory.persistance.DirEntryStore;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.mailbox.persistance.MailboxStore;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public abstract class AbstractMailboxServiceTests {

	protected MailboxStore mailboxStore;
	protected ItemStore itemStore;

	protected SecurityContext defaultSecurityContext;
	protected SecurityContext userSecurityContext;
	protected Container container;

	protected Container tagContainer;
	protected String domainUid;

	protected Server smtpServer;
	protected Server imapServer;
	protected Server imapServerNotAssigned;

	protected DirEntryStore dirEntryStore;

	private ItemValue<Server> dataLocation;
	protected String testUserUid;
	BmTestContext testContext;

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

		domainUid = "bm.lan";

		smtpServer = new Server();
		smtpServer.ip = new BmConfIni().get("smtp-role");
		smtpServer.tags = Lists.newArrayList("mail/smtp");

		imapServer = new Server();
		imapServer.ip = new BmConfIni().get("imap-role");
		imapServer.tags = Lists.newArrayList("mail/imap");

		imapServerNotAssigned = new Server();
		imapServerNotAssigned.ip = "3.3.3.3";
		imapServerNotAssigned.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(smtpServer, imapServer, imapServerNotAssigned);

		PopulateHelper.createTestDomain(domainUid, smtpServer, imapServer);

		// create domain parititon on cyrus
		new CyrusService(imapServer.ip).createPartition(domainUid);
		new CyrusService(imapServer.ip).refreshPartitions(Arrays.asList(domainUid));
		new CyrusAdmins(
				ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class, "default"),
				imapServer.ip).write();
		new CyrusService(imapServer.ip).reload();

		testUserUid = PopulateHelper.addUser("testuser" + System.currentTimeMillis(), domainUid);

		IServer serverService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				InstallationId.getIdentifier());

		dataLocation = serverService.getComplete(imapServer.ip);

		defaultSecurityContext = BmTestContext
				.contextWithSession("admin", "admin", domainUid, SecurityContext.ROLE_ADMIN,
						BasicRoles.ROLE_MANAGE_USER_SHARINGS, BasicRoles.ROLE_MANAGE_MAILSHARE_SHARINGS)
				.getSecurityContext();

		userSecurityContext = new SecurityContext("user", testUserUid, new ArrayList<String>(), Arrays.<String>asList(),
				domainUid);

		Sessions.get().put(userSecurityContext.getSessionId(), userSecurityContext);

		DataSource pool = JdbcTestHelper.getInstance().getDataSource();

		ContainerStore containerStore = new ContainerStore(pool, SecurityContext.SYSTEM);
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

}
