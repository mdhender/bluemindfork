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

import java.util.Arrays;
import java.util.UUID;

import org.apache.james.mime4j.stream.RawField;
import org.junit.Test;

import io.vertx.core.eventbus.Message;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.LocalJsonObject;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sendmail.Mail;
import net.bluemind.core.sendmail.SendMailAddress;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.vertx.VertxEventChecker;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class EmailHookTests extends AbstractHookTests {

	@Test
	public void testOwnerNotification() throws ServerFault {

		IUser userService = ServerSideServiceProvider.getProvider(context).instance(IUser.class, domainUid);

		ItemValue<User> admin = userService.byLogin("admin");
		assertNotNull(admin);

		IContainerManagement service = ServerSideServiceProvider.getProvider(context)
				.instance(IContainerManagement.class, "calendar:Default:" + admin.uid);

		VertxEventChecker<LocalJsonObject<Mail>> messageChecker = new VertxEventChecker<>(SendMailAddress.SEND);

		service.setAccessControlList(Arrays.asList(AccessControlEntry.create(admin.uid, Verb.Write)));

		messageChecker.shouldFail();
	}

	@Test
	public void testSendNotification() throws ServerFault {

		String uid = UUID.randomUUID().toString();
		defaultUser(uid, "test");

		IUser userService = ServerSideServiceProvider.getProvider(context).instance(IUser.class, domainUid);

		ItemValue<User> admin = userService.byLogin("admin");
		assertNotNull(admin);

		ItemValue<User> test = userService.byLogin("test");
		assertNotNull(test);

		IContainerManagement service = ServerSideServiceProvider.getProvider(context)
				.instance(IContainerManagement.class, "calendar:Default:" + admin.uid);

		VertxEventChecker<LocalJsonObject<Mail>> messageChecker = new VertxEventChecker<>(SendMailAddress.SEND);

		service.setAccessControlList(Arrays.asList(AccessControlEntry.create(test.uid, Verb.Write)));

		Message<LocalJsonObject<Mail>> message = messageChecker.shouldSuccess();
		assertNotNull(message);

		Mail mail = message.body().getValue();
		assertEquals("admin vous a partagé un calendrier", mail.subject);
		assertEquals("admin vous a partagé le calendrier \"admin\".", mail.html);

		RawField bmHeader = mail.headers.get(0);
		assertEquals("calendar:Default:" + admin.uid, bmHeader.getBody());
		assertEquals("no-reply@" + domainUid, mail.sender.getAddress());
		assertEquals("no-reply@" + domainUid, mail.from.getAddress());
		assertEquals("test@" + domainUid, mail.to.getAddress());
	}

	@Test
	public void testOwnerNotification_ByAdmin0() throws ServerFault {

		IUser userService = ServerSideServiceProvider.getProvider(context).instance(IUser.class, domainUid);

		ItemValue<User> admin = userService.byLogin("admin");
		assertNotNull(admin);

		SecurityContext sc = new SecurityContext("yay", "admin0", Arrays.<String>asList(),
				Arrays.<String>asList(SecurityContext.ROLE_SYSTEM), "global.virt");
		Sessions.get().put("yay", sc);
		IContainerManagement service = ServerSideServiceProvider.getProvider(sc).instance(IContainerManagement.class,
				"calendar:Default:" + admin.uid);

		VertxEventChecker<LocalJsonObject<Mail>> messageChecker = new VertxEventChecker<>(SendMailAddress.SEND);

		service.setAccessControlList(Arrays.asList(AccessControlEntry.create(admin.uid, Verb.Write)));

		messageChecker.shouldFail();
	}

	@Test
	public void testSendNotification_ByAdmin0() throws ServerFault {

		String uid = UUID.randomUUID().toString();
		defaultUser(uid, "test");

		IUser userService = ServerSideServiceProvider.getProvider(context).instance(IUser.class, domainUid);
		ItemValue<User> admin = userService.byLogin("admin");
		ItemValue<User> test = userService.byLogin("test");

		SecurityContext sc = new SecurityContext("yay", "admin0", Arrays.<String>asList(),
				Arrays.<String>asList(SecurityContext.ROLE_SYSTEM), "global.virt");
		Sessions.get().put("yay", sc);
		IContainerManagement service = ServerSideServiceProvider.getProvider(sc).instance(IContainerManagement.class,
				"calendar:Default:" + admin.uid);

		VertxEventChecker<LocalJsonObject<Mail>> messageChecker = new VertxEventChecker<>(SendMailAddress.SEND);
		service.setAccessControlList(Arrays.asList(AccessControlEntry.create(test.uid, Verb.Write)));
		Message<LocalJsonObject<Mail>> message = messageChecker.shouldSuccess();
		assertNotNull(message);
	}

}
