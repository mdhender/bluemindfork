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
package net.bluemind.mailbox.identity.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SettableFuture;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.identity.api.IMailboxIdentity;
import net.bluemind.mailbox.identity.api.Identity;
import net.bluemind.mailbox.identity.api.IdentityDescription;
import net.bluemind.mailbox.identity.api.SignatureFormat;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class MailboxIdentityTests {

	protected SecurityContext defaultSecurityContext;
	protected SecurityContext adminSecurityContext;

	protected String domainUid;
	private String mboxUid;
	private BmContext testContext;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		
		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();

		domainUid = "bm.lan";

		// register elasticsearch to locator
		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		String cyrusIp = new BmConfIni().get("imap-role");
		Server imapServer = new Server();
		imapServer.ip = cyrusIp;
		imapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(esServer, imapServer);

		PopulateHelper.createTestDomain(domainUid, esServer, imapServer);

		IServer serverService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				InstallationId.getIdentifier());

		ItemValue<Server> dataLocation = serverService.getComplete(cyrusIp);

		testContext = new BmTestContext(SecurityContext.SYSTEM);
		User user = new User();
		user.login = "testuser";
		user.routing = Routing.none;
		user.dataLocation = dataLocation.uid;
		user.password = "test";
		user.contactInfos = new VCard();
		user.contactInfos.identification.name = VCard.Identification.Name.create("test", "test", null, null, null,
				Collections.<VCard.Parameter>emptyList());
		user.emails = Arrays.asList(Email.create("test@bm.lan", true, true));
		testContext.provider().instance(IUser.class, domainUid).create("testUser", user);
		mboxUid = "testUser";
		defaultSecurityContext = BmTestContext
				.contextWithSession("testUser", "testUser", domainUid, BasicRoles.ROLE_MANAGE_MAILBOX_IDENTITIES)
				.getSecurityContext();

		adminSecurityContext = new SecurityContext(UUID.randomUUID().toString(), "system", Arrays.<String>asList(),
				Arrays.<String>asList(SecurityContext.ROLE_SYSTEM), "global.virt", "en", "internal-system");

		Sessions.get().put(adminSecurityContext.getSessionId(), adminSecurityContext);

		DataSource pool = JdbcTestHelper.getInstance().getDataSource();
		ContainerStore containerHome = new ContainerStore(pool, defaultSecurityContext);
		Container container = containerHome.get(domainUid);
		assertNotNull(container);

		AclStore aclStore = new AclStore(pool);
		aclStore.store(container,
				Arrays.asList(AccessControlEntry.create(defaultSecurityContext.getSubject(), Verb.All)));

		aclStore.store(containerHome.get(IMailboxAclUids.uidForMailbox(mboxUid)),
				Arrays.asList(AccessControlEntry.create(defaultSecurityContext.getSubject(), Verb.All)));

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCreate() throws ServerFault {
		IMailboxIdentity s = service(defaultSecurityContext, domainUid, mboxUid);
		s.create("work", defaultIdentity());
		assertNotNull(s.get("work"));

		try {
			s.create("work", defaultIdentity());
			fail("duplicate id created");
		} catch (ServerFault e) {
		}

		try {
			service(defaultSecurityContext, "fakeDomain", mboxUid).create("work", defaultIdentity());
			fail("should have fail");
		} catch (ServerFault e) {
		}

		try {
			service(defaultSecurityContext, domainUid, "fakeMboxUid").create("work", defaultIdentity());
			fail("should have fail");
		} catch (ServerFault e) {

		}
	}

	@Test
	public void testUpdate() throws ServerFault {
		IMailboxIdentity s = service(defaultSecurityContext, domainUid, mboxUid);
		s.create("work", defaultIdentity());
		s.update("work", defaultIdentity());

		try {
			service(defaultSecurityContext, domainUid, mboxUid).update("fakeId", defaultIdentity());
			fail("should have fail");
		} catch (ServerFault e) {

		}

		try {
			service(defaultSecurityContext, "fakeDomain", mboxUid).update("work", defaultIdentity());
			fail("should have fail");
		} catch (ServerFault e) {

		}
		try {
			service(defaultSecurityContext, domainUid, "fakeMboxUid").update("work", defaultIdentity());
			fail("should have fail");
		} catch (ServerFault e) {

		}
	}

	@Test
	public void testDelete() throws ServerFault {
		IMailboxIdentity s = service(defaultSecurityContext, domainUid, mboxUid);
		s.create("work", defaultIdentity());
		s.delete("work");
		assertNull(s.get("work"));
		try {
			s.delete("work");
			fail("should fail");
		} catch (ServerFault e) {

		}

	}

	@Test
	public void testGetPossibleIdentities() throws Exception {
		List<IdentityDescription> res = service(defaultSecurityContext, domainUid, mboxUid).getPossibleIdentities();
		assertEquals(2, res.size());

		IMailboxIdentity s = service(defaultSecurityContext, domainUid, mboxUid);
		s.create("work", defaultIdentity());

		res = service(defaultSecurityContext, domainUid, mboxUid).getPossibleIdentities();
		assertEquals(2, res.size());
		// one identity should have an id
		boolean hasId = false;
		hasId |= res.get(0).id != null;
		hasId |= res.get(1).id != null;
		assertTrue(hasId);

	}

	@Test
	public void testGetPossibleIdentitiesForGroup() throws Exception {

		Group g = new Group();
		g.emails = Arrays.asList(Email.create("groupTest@" + domainUid, true));
		g.name = "groupTest";
		String uid = "groupTest";
		testContext.provider().instance(IGroup.class, domainUid).create("groupTest", g);

		List<IdentityDescription> identities = service(adminSecurityContext, domainUid, "groupTest")
				.getPossibleIdentities();
		assertTrue(identities.isEmpty());
	}

	protected IMailboxIdentity service(SecurityContext sc, String domainUid, String mboxUid) throws ServerFault {
		return ServerSideServiceProvider.getProvider(sc).instance(IMailboxIdentity.class, domainUid, mboxUid);
	}

	private Identity defaultIdentity() {
		Identity i = new Identity();
		i.displayname = "test";
		i.name = "test";
		i.email = "test@" + domainUid;
		i.format = SignatureFormat.HTML;
		i.signature = "-- gg";
		i.sentFolder = "Sent";
		return i;
	}

}
