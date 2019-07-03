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
package net.bluemind.user.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.collect.Lists;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.directory.api.IOrgUnits;
import net.bluemind.directory.api.OrgUnit;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.identity.api.IdentityDescription;
import net.bluemind.mailbox.identity.api.SignatureFormat;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUserMailIdentities;
import net.bluemind.user.api.User;
import net.bluemind.user.api.UserMailIdentity;

public class UserMailIdentityTests {

	protected String domainUid;
	private SecurityContext userSecurityContext;
	private String userUid;
	private String user2Uid;
	private SecurityContext user2SecurityContext;
	private BmTestContext testContext;
	private SecurityContext adminSecurityContext;
	private SecurityContext userWithoutSelfChangeMailIdentiesSecurityContext;
	private String adminTlseUid;
	private SecurityContext adminTlseSecuriyContext;

	@Before
	public void before() throws Exception {
		domainUid = "bm.lan";

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

		testContext = new BmTestContext(SecurityContext.SYSTEM);
		// register elasticsearch to locator
		Server esServer = new Server();
		esServer.ip = new BmConfIni().get("es-host");
		esServer.tags = Lists.newArrayList("bm/es");

		String cyrusIp = new BmConfIni().get("imap-role");
		Server imapServer = new Server();
		imapServer.ip = cyrusIp;
		imapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(esServer, imapServer);

		PopulateHelper.createTestDomain(domainUid);
		PopulateHelper.unAssignFakeCyrus(domainUid);
		testContext.provider().instance(IServer.class, InstallationId.getIdentifier()).assign(imapServer.ip, domainUid,
				"mail/imap");

		testContext.provider().instance(IOrgUnits.class, domainUid).create("tlse", OrgUnit.create("tlse", null));

		userUid = PopulateHelper.addUser("test", domainUid);
		User user2 = PopulateHelper.getUser("test2", domainUid, Mailbox.Routing.none);
		user2.orgUnitUid = "tlse";
		user2Uid = PopulateHelper.addUser(domainUid, user2);
		adminTlseUid = PopulateHelper.addUser("test3", domainUid);

		userSecurityContext = BmTestContext
				.contextWithSession("userSessionId", userUid, domainUid, BasicRoles.ROLE_SELF_CHANGE_MAIL_IDENTITIES)
				.getSecurityContext();

		userWithoutSelfChangeMailIdentiesSecurityContext = BmTestContext
				.contextWithSession("userWithoutMailIdentitiesSessionId", userUid, domainUid).getSecurityContext();

		user2SecurityContext = BmTestContext
				.contextWithSession("user2SessionId", user2Uid, domainUid, BasicRoles.ROLE_SELF_CHANGE_MAIL_IDENTITIES)
				.getSecurityContext();

		adminTlseSecuriyContext = BmTestContext
				.context(adminTlseUid, domainUid, BasicRoles.ROLE_SELF_CHANGE_MAIL_IDENTITIES)
				.withRolesOnOrgUnit("tlse", BasicRoles.ROLE_MANAGE_USER)//
				.session("adminTlseUid").getSecurityContext();

		adminSecurityContext = BmTestContext
				.contextWithSession("adminSessionId", "adminUid", domainUid, BasicRoles.ROLE_ADMIN)
				.getSecurityContext();

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCreate() throws Exception {

		service(userSecurityContext, userUid).create("work", defaultIdentity("test@bm.lan", userUid));

		try {
			service(userSecurityContext, userUid).create("work", defaultIdentity("test@bm.lan", userUid));
			fail("duplicate id created");
		} catch (ServerFault e) {
		}

		try {
			service(userSecurityContext, userUid).create("work2", defaultIdentity("test2@bm.lan", user2Uid));
			fail("should fail because of forbidden");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		try {
			service(userWithoutSelfChangeMailIdentiesSecurityContext, userUid).create("work2",
					defaultIdentity("test2@bm.lan", user2Uid));
			fail("should fail because of forbidden");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

	}

	@Test
	public void testUpdate() throws Exception {

		service(userSecurityContext, userUid).create("work", defaultIdentity("test@bm.lan", userUid));

		service(userSecurityContext, userUid).update("work", defaultIdentity("test@bm.lan", userUid));

		try {
			service(userSecurityContext, userUid).update("work2", defaultIdentity("test2@bm.lan", user2Uid));
			fail("should fail because of forbidden");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		try {
			service(userWithoutSelfChangeMailIdentiesSecurityContext, userUid).update("work2",
					defaultIdentity("test@bm.lan", userUid));
			fail("should fail because of forbidden");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void testDelete() throws Exception {

		service(userSecurityContext, userUid).create("work", defaultIdentity("test@bm.lan", userUid));

		try {
			service(user2SecurityContext, userUid).delete("work");
			fail("should fail because of forbidden");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		try {
			service(userWithoutSelfChangeMailIdentiesSecurityContext, userUid).delete("work");
			fail("should fail because of forbidden");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		service(userSecurityContext, userUid).delete("work");
	}

	@Test
	public void testGetAvailableIdenties() throws Exception {
		List<IdentityDescription> res = service(userSecurityContext, userUid).getAvailableIdentities();
		assertEquals(1, res.size());

		// give Write right on user2 mbox to user1
		testContext.provider().instance(IContainerManagement.class, IMailboxAclUids.uidForMailbox(user2Uid))
				.setAccessControlList(Arrays.asList(AccessControlEntry.create(userUid, Verb.Write)));

		res = service(userSecurityContext, userUid).getAvailableIdentities();
		assertEquals(2, res.size());

		res = service(userWithoutSelfChangeMailIdentiesSecurityContext, userUid).getAvailableIdentities();
		assertEquals(2, res.size());

		// avaibleIdentities from with admin user
		res = service(adminSecurityContext, userUid).getAvailableIdentities();
		assertEquals(2, res.size());

	}

	@Test
	public void testGetAvailableIdentiesWithOUAdmin() throws Exception {
		// admin toulouse doesnt have right to manage userUid mailbox

		List<IdentityDescription> res = service(adminTlseSecuriyContext, user2Uid).getAvailableIdentities();
		assertEquals(1, res.size());

		// give Write right on user1 mbox to user2
		testContext.provider().instance(IContainerManagement.class, IMailboxAclUids.uidForMailbox(userUid))
				.setAccessControlList(Arrays.asList(AccessControlEntry.create(user2Uid, Verb.Write)));

		// admin toulouse doesnt have right to manage userUid mailbox but he can
		// access userUid mailbox identities thru user2 available identities

		res = service(adminTlseSecuriyContext, user2Uid).getAvailableIdentities();
		assertEquals(2, res.size());

	}

	protected IUserMailIdentities service(SecurityContext sc, String userUid) throws ServerFault {
		return ServerSideServiceProvider.getProvider(sc).instance(IUserMailIdentities.class, domainUid, userUid);
	}

	private UserMailIdentity defaultIdentity(String email, String mboxUid) {
		UserMailIdentity i = new UserMailIdentity();
		i.displayname = "test";
		i.name = "test";
		i.email = email;
		i.format = SignatureFormat.HTML;
		i.signature = "-- gg";
		i.mailboxUid = mboxUid;
		i.sentFolder = "Sent";
		return i;
	}
}
