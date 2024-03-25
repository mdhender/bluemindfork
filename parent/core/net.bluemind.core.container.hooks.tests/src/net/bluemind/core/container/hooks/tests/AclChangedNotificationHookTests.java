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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import io.vertx.core.eventbus.Message;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.hooks.aclchangednotification.AclChangedMsg;
import net.bluemind.core.container.hooks.aclchangednotification.AclChangedNotificationVerticle;
import net.bluemind.core.container.hooks.aclchangednotification.AclWithStatus;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.rest.LocalJsonObject;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.vertx.VertxEventChecker;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class AclChangedNotificationHookTests extends AbstractHookTests {

	@Test
	public void testOwnerNotification_add_acl() throws ServerFault {

		IUser userService = ServerSideServiceProvider.getProvider(context).instance(IUser.class, domainUid);

		ItemValue<User> admin = userService.byLogin("admin");
		assertNotNull(admin);

		String uid = UUID.randomUUID().toString();
		defaultUser(uid, "test1");
		ItemValue<User> test1 = userService.byLogin("test1");
		assertNotNull(test1);

		uid = UUID.randomUUID().toString();
		defaultUser(uid, "test2");
		ItemValue<User> test2 = userService.byLogin("test2");
		assertNotNull(test2);

		IContainerManagement service = ServerSideServiceProvider.getProvider(context)
				.instance(IContainerManagement.class, "calendar:Default:" + admin.uid);

		VertxEventChecker<LocalJsonObject<AclChangedMsg>> messageChecker = new VertxEventChecker<>(
				AclChangedNotificationVerticle.ACL_CHANGED_NOTIFICATION_COLLECT_BUS_ADDRESS);

		service.setAccessControlList(Arrays.asList(AccessControlEntry.create(test1.uid, Verb.Write),
				AccessControlEntry.create(test2.uid, Verb.Read)));

		Message<LocalJsonObject<AclChangedMsg>> message = messageChecker.shouldSuccess();
		assertNotNull(message);

		AclChangedMsg mail = message.body().getValue();
		assertEquals("calendar", mail.containerType());
		assertTrue(mail.changes().stream().anyMatch(d -> d.entry().subject.equals(test1.uid)
				&& d.status() == AclWithStatus.AclStatus.ADDED && d.entry().verb == Verb.Write));
		assertTrue(mail.changes().stream().anyMatch(d -> d.entry().subject.equals(test2.uid)
				&& d.status() == AclWithStatus.AclStatus.ADDED && d.entry().verb == Verb.Read));
	}

	@Test
	public void testOwnerNotification_remove_acl() throws ServerFault {

		IUser userService = ServerSideServiceProvider.getProvider(context).instance(IUser.class, domainUid);

		ItemValue<User> admin = userService.byLogin("admin");
		assertNotNull(admin);

		String uid = UUID.randomUUID().toString();
		defaultUser(uid, "test1");
		ItemValue<User> test1 = userService.byLogin("test1");
		assertNotNull(test1);

		uid = UUID.randomUUID().toString();
		defaultUser(uid, "test2");
		ItemValue<User> test2 = userService.byLogin("test2");
		assertNotNull(test2);

		IContainerManagement service = ServerSideServiceProvider.getProvider(context)
				.instance(IContainerManagement.class, "calendar:Default:" + admin.uid);

		VertxEventChecker<LocalJsonObject<AclChangedMsg>> messageChecker = new VertxEventChecker<>(
				AclChangedNotificationVerticle.ACL_CHANGED_NOTIFICATION_COLLECT_BUS_ADDRESS);

		List<AccessControlEntry> accessControlList = service.getAccessControlList();
		accessControlList.add(AccessControlEntry.create(test1.uid, Verb.Write));
		accessControlList.add(AccessControlEntry.create(test2.uid, Verb.Read));
		service.setAccessControlList(accessControlList);

		Message<LocalJsonObject<AclChangedMsg>> message = messageChecker.shouldSuccess();
		assertNotNull(message);

		AclChangedMsg mail = message.body().getValue();
		assertEquals("calendar", mail.containerType());
		assertTrue(mail.changes().stream().anyMatch(d -> d.entry().subject.equals(test1.uid)
				&& d.status() == AclWithStatus.AclStatus.ADDED && d.entry().verb == Verb.Write));
		assertTrue(mail.changes().stream().anyMatch(d -> d.entry().subject.equals(test2.uid)
				&& d.status() == AclWithStatus.AclStatus.ADDED && d.entry().verb == Verb.Read));

		VertxEventChecker<LocalJsonObject<AclChangedMsg>> messageChecker2 = new VertxEventChecker<>(
				AclChangedNotificationVerticle.ACL_CHANGED_NOTIFICATION_COLLECT_BUS_ADDRESS);

		accessControlList = service.getAccessControlList();
		accessControlList.removeIf(ace -> ace.subject.equals(test2.uid) && ace.verb == Verb.Read);
		service.setAccessControlList(accessControlList);

		message = messageChecker2.shouldSuccess();
		assertNotNull(message);

		mail = message.body().getValue();
		assertEquals("calendar", mail.containerType());
		assertFalse(mail.changes().stream().anyMatch(d -> d.entry().subject.equals(test1.uid)
				&& d.status() == AclWithStatus.AclStatus.ADDED && d.entry().verb == Verb.Write));
		assertTrue(mail.changes().stream().anyMatch(d -> d.entry().subject.equals(test2.uid)
				&& d.status() == AclWithStatus.AclStatus.REMOVED && d.entry().verb == Verb.Read));
	}

	@Test
	public void testOwnerNotification_update_acl() throws ServerFault {

		IUser userService = ServerSideServiceProvider.getProvider(context).instance(IUser.class, domainUid);

		ItemValue<User> admin = userService.byLogin("admin");
		assertNotNull(admin);

		String uid = UUID.randomUUID().toString();
		defaultUser(uid, "test1");
		ItemValue<User> test1 = userService.byLogin("test1");
		assertNotNull(test1);

		uid = UUID.randomUUID().toString();
		defaultUser(uid, "test2");
		ItemValue<User> test2 = userService.byLogin("test2");
		assertNotNull(test2);

		IContainerManagement service = ServerSideServiceProvider.getProvider(context)
				.instance(IContainerManagement.class, "calendar:Default:" + admin.uid);

		VertxEventChecker<LocalJsonObject<AclChangedMsg>> messageChecker = new VertxEventChecker<>(
				AclChangedNotificationVerticle.ACL_CHANGED_NOTIFICATION_COLLECT_BUS_ADDRESS);

		List<AccessControlEntry> accessControlList = service.getAccessControlList();
		accessControlList.add(AccessControlEntry.create(test1.uid, Verb.Read));
		accessControlList.add(AccessControlEntry.create(test2.uid, Verb.Read));
		service.setAccessControlList(accessControlList);

		Message<LocalJsonObject<AclChangedMsg>> message = messageChecker.shouldSuccess();
		assertNotNull(message);

		AclChangedMsg mail = message.body().getValue();
		assertEquals("calendar", mail.containerType());
		assertTrue(mail.changes().stream().anyMatch(d -> d.entry().subject.equals(test1.uid)
				&& d.status() == AclWithStatus.AclStatus.ADDED && d.entry().verb == Verb.Read));
		assertTrue(mail.changes().stream().anyMatch(d -> d.entry().subject.equals(test2.uid)
				&& d.status() == AclWithStatus.AclStatus.ADDED && d.entry().verb == Verb.Read));

		VertxEventChecker<LocalJsonObject<AclChangedMsg>> messageChecker2 = new VertxEventChecker<>(
				AclChangedNotificationVerticle.ACL_CHANGED_NOTIFICATION_COLLECT_BUS_ADDRESS);

		accessControlList = service.getAccessControlList();
		accessControlList.removeIf(ace -> ace.subject.equals(test2.uid) && ace.verb == Verb.Read);
		accessControlList.add(AccessControlEntry.create(test2.uid, Verb.Write));
		service.setAccessControlList(accessControlList);

		message = messageChecker2.shouldSuccess();
		assertNotNull(message);

		mail = message.body().getValue();
		assertEquals("calendar", mail.containerType());
		assertTrue(mail.changes().stream().anyMatch(d -> d.entry().subject.equals(test2.uid)
				&& d.status() == AclWithStatus.AclStatus.REMOVED && d.entry().verb == Verb.Read));
		assertTrue(mail.changes().stream().anyMatch(d -> d.entry().subject.equals(test2.uid)
				&& d.status() == AclWithStatus.AclStatus.ADDED && d.entry().verb == Verb.Write));
	}

}
