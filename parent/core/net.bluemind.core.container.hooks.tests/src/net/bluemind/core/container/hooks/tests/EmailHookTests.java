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
package net.bluemind.core.container.hooks.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.eventbus.Message;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.hooks.aclchangednotification.AclChangedMsg;
import net.bluemind.core.container.hooks.aclchangednotification.AclChangedNotificationVerticle;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.rest.LocalJsonObject;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sendmail.Mail;
import net.bluemind.core.sendmail.SendMailAddress;
import net.bluemind.core.tests.vertx.VertxEventChecker;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class EmailHookTests extends AbstractHookTests {
	@After
	@Before
	public void afterTestFlushOut() {
		VertxPlatform.eventBus().request(AclChangedNotificationVerticle.ACL_CHANGED_NOTIFICATION_TEARDOWN_BUS_ADDRESS,
				null);
	}

	@Test
	public void test_SameUserOwnerAcl_shouldFail() throws ServerFault {
		// as admin set ACL on admin container to admin user => do not notify admin

		IUser userService = ServerSideServiceProvider.getProvider(context).instance(IUser.class, domainUid);

		ItemValue<User> admin = userService.byLogin("admin");
		assertNotNull(admin);

		IContainerManagement service = ServerSideServiceProvider.getProvider(context)
				.instance(IContainerManagement.class, "calendar:Default:" + admin.uid);

		VertxEventChecker<LocalJsonObject<Mail>> messageChecker = new VertxEventChecker<>(SendMailAddress.SEND);

		List<AccessControlEntry> aclList = service.getAccessControlList();
		aclList.add(AccessControlEntry.create(admin.uid, Verb.Write));
		service.setAccessControlList(aclList);

		messageChecker.shouldFail();
	}

	@Test
	public void test_SameUserAcl_shouldFail() throws ServerFault {
		// as admin set ACL on test container to admin user => do not notify admin user

		String uid = UUID.randomUUID().toString();
		defaultUser(uid, "test");

		IUser userService = ServerSideServiceProvider.getProvider(context).instance(IUser.class, domainUid);

		ItemValue<User> admin = userService.byLogin("admin");
		assertNotNull(admin);

		ItemValue<User> test = userService.byLogin("test");
		assertNotNull(test);

		IContainerManagement service = ServerSideServiceProvider.getProvider(context)
				.instance(IContainerManagement.class, "calendar:Default:" + test.uid);

		VertxEventChecker<LocalJsonObject<Mail>> messageChecker = new VertxEventChecker<>(SendMailAddress.SEND);

		List<AccessControlEntry> aclList = service.getAccessControlList();
		aclList.add(AccessControlEntry.create(admin.uid, Verb.Write));
		service.setAccessControlList(aclList);

		messageChecker.shouldFail();
	}

	@Test
	public void test_SameUserOwner_shouldSuccess() throws ServerFault {
		// as admin set ACL on admin container to test user => notify test user

		String uid = UUID.randomUUID().toString();
		defaultUser(uid, "test");

		IUser userService = ServerSideServiceProvider.getProvider(context).instance(IUser.class, domainUid);

		ItemValue<User> admin = userService.byLogin("admin");
		assertNotNull(admin);

		ItemValue<User> test = userService.byLogin("test");
		assertNotNull(test);

		IContainerManagement service = ServerSideServiceProvider.getProvider(context)
				.instance(IContainerManagement.class, "calendar:Default:" + admin.uid);

		VertxEventChecker<LocalJsonObject<AclChangedMsg>> messageAclChecker = new VertxEventChecker<>(
				AclChangedNotificationVerticle.ACL_CHANGED_NOTIFICATION_COLLECT_BUS_ADDRESS);
		VertxEventChecker<LocalJsonObject<Mail>> messageChecker = new VertxEventChecker<>(SendMailAddress.SEND);

		List<AccessControlEntry> aclList = service.getAccessControlList();
		aclList.add(AccessControlEntry.create(test.uid, Verb.Write));
		service.setAccessControlList(aclList);

		messageAclChecker.shouldSuccess();
		Message<LocalJsonObject<Mail>> message = messageChecker.shouldSuccess();
		assertNotNull(message);
		Mail mail = message.body().getValue();
		assertEquals("admin a modifié vos droits d’accés.", mail.subject);
		String[] fields = mail.getMessage().getHeader().getFields("X-BM-FolderUid").getFirst().getBody().split("; ");
		assertEquals("type=calendar", fields[1]);
		assertEquals("calendar:Default:admin", fields[0]);
	}

	@Test
	public void test_SameOwnerAcl_shouldSuccess() throws ServerFault {
		// as admin0 set ACL on test container to test user => notify test user

		String uid = UUID.randomUUID().toString();
		defaultUser(uid, "test");

		IUser userService = ServerSideServiceProvider.getProvider(context).instance(IUser.class, domainUid);

		ItemValue<User> test = userService.byLogin("test");
		assertNotNull(test);

		IContainerManagement service = ServerSideServiceProvider.getProvider(admin0Context)
				.instance(IContainerManagement.class, "calendar:Default:" + test.uid);

		VertxEventChecker<LocalJsonObject<AclChangedMsg>> messageAclChecker = new VertxEventChecker<>(
				AclChangedNotificationVerticle.ACL_CHANGED_NOTIFICATION_COLLECT_BUS_ADDRESS);
		VertxEventChecker<LocalJsonObject<Mail>> messageChecker = new VertxEventChecker<>(SendMailAddress.SEND);

		List<AccessControlEntry> aclList = service.getAccessControlList();
		aclList.removeIf(a -> a.subject.equals(test.uid));
		service.setAccessControlList(aclList);

		messageAclChecker.shouldSuccess();
		Message<LocalJsonObject<Mail>> message = messageChecker.shouldSuccess();
		assertNotNull(message);
		Mail mail = message.body().getValue();
		assertEquals("L’administrateur a modifié vos droits d’accés.", mail.subject);
		String[] fields = mail.getMessage().getHeader().getFields("X-BM-FolderUid").getFirst().getBody().split("; ");
		assertEquals("type=calendar", fields[1]);
		assertEquals("calendar:Default:" + test.uid, fields[0]);
	}

	@Test
	public void test_DifferentUserOwnerAcl_shouldSuccess() throws Exception {
		// as admin0 set ACL on admin container to test user => notify test user

		String uid = UUID.randomUUID().toString();
		defaultUser(uid, "test");

		IUser userService = ServerSideServiceProvider.getProvider(context).instance(IUser.class, domainUid);

		ItemValue<User> test = userService.byLogin("test");
		assertNotNull(test);

		ItemValue<User> admin = userService.byLogin("admin");
		assertNotNull(admin);

		IContainerManagement service = ServerSideServiceProvider.getProvider(admin0Context)
				.instance(IContainerManagement.class, "calendar:Default:" + admin.uid);

		VertxEventChecker<LocalJsonObject<AclChangedMsg>> messageAclChecker = new VertxEventChecker<>(
				AclChangedNotificationVerticle.ACL_CHANGED_NOTIFICATION_COLLECT_BUS_ADDRESS);
		VertxEventChecker<LocalJsonObject<Mail>> messageChecker = new VertxEventChecker<>(SendMailAddress.SEND);

		List<AccessControlEntry> aclList = service.getAccessControlList();
		aclList.add(AccessControlEntry.create(test.uid, Verb.Write));
		service.setAccessControlList(aclList);

		messageAclChecker.shouldSuccess();
		Message<LocalJsonObject<Mail>> message = messageChecker.shouldSuccess();
		assertNotNull(message);
		Mail mail = message.body().getValue();
		assertEquals("L’administrateur a modifié vos droits d’accés.", mail.subject);
		String[] fields = mail.getMessage().getHeader().getFields("X-BM-FolderUid").getFirst().getBody().split("; ");
		assertEquals("type=calendar", fields[1]);
		assertEquals("calendar:Default:admin", fields[0]);
	}

}
