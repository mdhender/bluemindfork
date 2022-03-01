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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.directory.api.IOrgUnits;
import net.bluemind.directory.api.OrgUnit;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.group.api.Member.Type;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.identity.api.IdentityDescription;
import net.bluemind.mailbox.identity.api.SignatureFormat;
import net.bluemind.mailshare.api.IMailshare;
import net.bluemind.mailshare.api.Mailshare;
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
	private String mailshareUid;
	private String groupUid;
	private SecurityContext userWithGroupSecurityContext;

	@Before
	public void before() throws Exception {
		domainUid = "bm.lan";

		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

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

		IGroup groupService = testContext.provider().instance(IGroup.class, domainUid);
		Group group = new Group();
		group.name = "my-test-group";
		groupService.create("test-group", group);
		groupUid = groupService.allUids().get(0);

		Member member1 = new Member();
		member1.type = Type.user;
		member1.uid = userUid;
		Member member2 = new Member();
		member2.type = Type.user;
		member2.uid = user2Uid;
		groupService.add(groupUid, Arrays.asList(member1, member2));

		IMailshare mailshareService = testContext.provider().instance(IMailshare.class, domainUid);
		Mailshare mailshare = new Mailshare();
		mailshare.name = "my-test-mailshare";
		mailshare.emails = Arrays.asList(Email.create(mailshare.name + "@" + domainUid, true));
		mailshare.routing = Routing.internal;
		mailshareService.create("test-mailshare", mailshare);

		List<ItemValue<Mailshare>> allComplete = mailshareService.allComplete();
		mailshareUid = allComplete.get(0).uid;

		userSecurityContext = BmTestContext
				.contextWithSession("userSessionId", userUid, domainUid, BasicRoles.ROLE_SELF_CHANGE_MAIL_IDENTITIES)
				.getSecurityContext();

		userWithGroupSecurityContext = BmTestContext.contextWithSession("userWithGroupSessionId", userUid, domainUid)
				.withGroup(groupUid).getSecurityContext();
		Sessions.get().put("userWithGroupSessionId", userWithGroupSecurityContext);

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
	public void testDeleteDefault() throws Exception {

		UserMailIdentity defaultIdentity = defaultIdentity("test@bm.lan", userUid);
		service(userSecurityContext, userUid).create("work", defaultIdentity);
		service(userSecurityContext, userUid).setDefault("work");

		try {
			service(userSecurityContext, userUid).delete("work");
			fail("should fail because delete default identity is forbidden");
		} catch (ServerFault e) {
			assertEquals(String.format("Default identity %s cannot be deleted", defaultIdentity.displayname),
					e.getMessage());
		}

	}

	@Test
	public void testGetAvailableIdentiesGroup() throws Exception {
		List<IdentityDescription> res = service(userWithGroupSecurityContext, userUid).getAvailableIdentities();
		assertEquals(1, res.size());

		// give ALL right on share mbox to group
		testContext.provider().instance(IContainerManagement.class, IMailboxAclUids.uidForMailbox(mailshareUid))
				.setAccessControlList(Arrays.asList(AccessControlEntry.create(groupUid, Verb.All)));

		List<AccessControlEntry> accessControlList = testContext.provider()
				.instance(IContainerManagement.class, IMailboxAclUids.uidForMailbox(mailshareUid))
				.getAccessControlList();
		assertEquals(1, accessControlList.size());

		List<IdentityDescription> resWithUserContext = service(userWithGroupSecurityContext, userUid)
				.getAvailableIdentities();
		List<IdentityDescription> resWithAdminContext = service(adminSecurityContext, userUid).getAvailableIdentities();

		assertEquals(resWithUserContext.size(), resWithAdminContext.size());
		assertTrue(resWithUserContext.stream().anyMatch(r -> r.name.equalsIgnoreCase("my-test-mailshare")));
		assertTrue(resWithAdminContext.stream().anyMatch(r -> r.name.equalsIgnoreCase("my-test-mailshare")));
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
