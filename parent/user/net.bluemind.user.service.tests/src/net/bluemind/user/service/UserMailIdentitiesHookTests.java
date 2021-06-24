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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.identity.api.IdentityDescription;
import net.bluemind.mailbox.identity.api.SignatureFormat;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserMailIdentities;
import net.bluemind.user.api.User;
import net.bluemind.user.api.UserMailIdentity;

public class UserMailIdentitiesHookTests {

	private String domainUid;
	private BmTestContext testContext;
	private String userUid;

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

		userUid = PopulateHelper.addUser("test", domainUid, Mailbox.Routing.internal);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCreateMBoxShouldCreateDefaultIdentity() {
		assertEquals(1, service().getIdentities().size());
		assertTrue(service().getIdentities().get(0).isDefault);
	}

	@Test
	public void testUpdateMBoxShouldCreateDefaultIdentityIfThereIsNoDefaultPresent() {
		assertEquals(1, service().getIdentities().size());
		IdentityDescription identityDescription = service().getIdentities().get(0);
		assertTrue(identityDescription.isDefault);

		service().delete(identityDescription.id);
		assertEquals(0, service().getIdentities().size());

		userService().update(userUid, userService().getComplete(userUid).value);

		assertEquals(1, service().getIdentities().size());
		assertTrue(service().getIdentities().get(0).isDefault);
	}

	@Test
	public void testIdentityPointingToNonExistingEmailShouldGetDeleted() {
		ItemValue<User> user = userService().getComplete(userUid);
		user.value.emails.add(Email.create("user-1@" + domainUid, false));
		userService().update(user.uid, user.value);

		assertEquals(1, service().getIdentities().size());

		String id = UUID.randomUUID().toString();
		UserMailIdentity identity = defaultIdentity("test1", "user-1@" + domainUid);
		service().create(id, identity);

		assertEquals(2, service().getIdentities().size());
		assertExists(id);

		removeMail(user, "user-1@" + domainUid);
		userService().update(user.uid, user.value);

		assertEquals(1, service().getIdentities().size());
		assertDoesNotExist(id);
	}

	@Test
	public void testDefaultIdentityPointingToNonExistingEmailShouldGetPointedToDefaultEmail() {
		ItemValue<User> user = userService().getComplete(userUid);
		user.value.emails.add(Email.create("user-1@" + domainUid, false));
		userService().update(user.uid, user.value);

		assertEquals(1, service().getIdentities().size());

		String id = UUID.randomUUID().toString();
		UserMailIdentity identity = defaultIdentity("test1", "user-1@" + domainUid);
		service().create(id, identity);
		service().setDefault(id);

		assertEquals(2, service().getIdentities().size());
		assertExists(id);

		removeMail(user, "user-1@" + domainUid);
		userService().update(user.uid, user.value);

		assertEquals(2, service().getIdentities().size());
		assertExists(id);

		identity = service().get(id);
		assertEquals("test@bm.lan", identity.email);
	}

	@Test
	public void testDefaultIdentityShouldFollowDefaultEmail() {
		ItemValue<User> user = userService().getComplete(userUid);
		user.value.emails.add(Email.create("user-1@" + domainUid, false));
		userService().update(user.uid, user.value);

		assertEquals(1, service().getIdentities().size());

		String id = UUID.randomUUID().toString();
		UserMailIdentity identity = defaultIdentity("test1", "test@bm.lan");
		service().create(id, identity);
		service().setDefault(id);

		assertEquals(2, service().getIdentities().size());
		assertExists(id);

		setDefaultMail(user, "user-1@" + domainUid);
		userService().update(user.uid, user.value);

		assertEquals(2, service().getIdentities().size());
		assertExists(id);

		identity = service().get(id);
		assertEquals("user-1@" + domainUid, identity.email);
		assertTrue(identity.isDefault);
	}

	@Test
	public void testRemovingOwnerACLsShouldLeaveIdentitiesUntouched() {
		IContainerManagement containerService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainerManagement.class, IMailboxAclUids.uidForMailbox(userUid));

		AccessControlEntry me = new AccessControlEntry();
		me.subject = userUid;
		me.verb = Verb.All;
		containerService.setAccessControlList(Arrays.asList(me));
		assertEquals(1, service().getIdentities().size());

		containerService.setAccessControlList(Collections.emptyList());
		assertEquals(1, service().getIdentities().size());
	}

	@Test
	public void testRemovingGroupWithOwnerShouldLeaveIdentitiesUntouched() {
		assertEquals(1, service().getIdentities().size());

		IContainerManagement containerService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainerManagement.class, IMailboxAclUids.uidForMailbox(userUid));

		IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domainUid);
		String groupUid = UUID.randomUUID().toString();
		Group group = new Group();
		group.name = "group" + "-" + System.nanoTime();
		group.description = "Test group";
		Email e = new Email();
		e.address = group.name + "@" + domainUid;
		e.allAliases = true;
		e.isDefault = true;
		group.emails = new ArrayList<Email>(1);
		group.emails.add(e);
		group.mailArchived = false;
		group.dataLocation = new BmConfIni().get("imap-role");
		groupService.create(groupUid, group);
		Member m = Member.user(userUid);
		groupService.add(groupUid, Arrays.asList(m));

		AccessControlEntry groupAce = new AccessControlEntry();
		groupAce.subject = groupUid;
		groupAce.verb = Verb.All;
		containerService.setAccessControlList(Arrays.asList(groupAce));
		assertEquals(1, service().getIdentities().size());

		containerService.setAccessControlList(Collections.emptyList());
		assertEquals(1, service().getIdentities().size());
	}

	private void assertExists(String id) {
		assertFalse(
				service().getIdentities().stream().filter(i -> i.id.equals(id)).collect(Collectors.toList()).isEmpty());
	}

	private void assertDoesNotExist(String id) {
		assertTrue(
				service().getIdentities().stream().filter(i -> i.id.equals(id)).collect(Collectors.toList()).isEmpty());
	}

	private void removeMail(ItemValue<User> user, String email) {
		Iterator<Email> iter = user.value.emails.iterator();
		while (iter.hasNext()) {
			if (iter.next().address.equals(email)) {
				iter.remove();
			}
		}

	}

	private void setDefaultMail(ItemValue<User> user, String email) {
		Iterator<Email> iter = user.value.emails.iterator();
		while (iter.hasNext()) {
			Email next = iter.next();
			if (next.address.equals(email)) {
				next.isDefault = true;
			} else {
				next.isDefault = false;
			}
		}

	}

	private UserMailIdentity defaultIdentity(String name, String email) {
		UserMailIdentity identity = new UserMailIdentity();
		identity.displayname = name;
		identity.email = email;
		identity.format = SignatureFormat.HTML;
		identity.isDefault = false;
		identity.name = identity.displayname;
		identity.sentFolder = "SENT";
		identity.signature = "sig";
		identity.mailboxUid = userUid;
		return identity;
	}

	protected IUserMailIdentities service() throws ServerFault {
		return ServerSideServiceProvider.getProvider(testContext).instance(IUserMailIdentities.class, domainUid,
				userUid);
	}

	protected IUser userService() throws ServerFault {
		return ServerSideServiceProvider.getProvider(testContext).instance(IUser.class, domainUid);
	}

}
