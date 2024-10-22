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
package net.bluemind.mailbox.hook;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.Email;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class MailboxHookTests {

	private String domainUid;
	private SecurityContext ctx;
	private ItemValue<Server> dataLocation;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());
		ctx = SecurityContext.SYSTEM;

		Server imapServer = new Server();
		imapServer.tags = Collections.singletonList(TagDescriptor.mail_imap.getTag());
		imapServer.ip = PopulateHelper.FAKE_CYRUS_IP;

		PopulateHelper.initGlobalVirt(imapServer);

		domainUid = "test" + System.nanoTime() + ".fr";

		IServer serverService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				InstallationId.getIdentifier());

		dataLocation = serverService.getComplete(PopulateHelper.FAKE_CYRUS_IP);

		PopulateHelper.createTestDomain(domainUid, imapServer);

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testHooksAreCalled() throws Exception {
		IMailboxes mailbox = getService();
		String uid = "mailbox" + System.nanoTime();
		mailbox.create(uid, defaultMailbox(uid));
		mailbox.update(uid, defaultMailbox(uid));
		mailbox.delete(uid);
		mailbox.setDomainFilter(new MailFilter());
		assertTrue(TestHook.latch.await(15, TimeUnit.SECONDS));
	}

	private Mailbox defaultMailbox(String name) {
		Mailbox mailbox = new Mailbox();
		mailbox.type = Mailbox.Type.user;
		mailbox.name = name;
		Email em = new Email();
		em.address = name + "@" + domainUid;
		em.isDefault = true;
		em.allAliases = false;
		mailbox.emails = Arrays.asList(em);

		mailbox.routing = Mailbox.Routing.none;
		mailbox.dataLocation = dataLocation.uid;
		return mailbox;
	}

	private IMailboxes getService() throws Exception {
		return ServerSideServiceProvider.getProvider(ctx).instance(IMailboxes.class, domainUid);
	}

}
