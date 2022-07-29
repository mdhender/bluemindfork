/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.user.hook.identity;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.core.api.Email;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.identity.api.SignatureFormat;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserMailIdentities;
import net.bluemind.user.api.User;
import net.bluemind.user.api.UserMailIdentity;

public class UserMailIdentityHookTests {

	private String domainUid;
	private SecurityContext ctx;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());
		ctx = SecurityContext.SYSTEM;

		domainUid = "test" + System.nanoTime() + ".fr";

		PopulateHelper.initGlobalVirt();
		PopulateHelper.createTestDomain(domainUid);

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
	}

	@Test
	public void testHooksAreCalled() throws Exception {
		IUser user = getUserService();
		String userUid = "user" + System.nanoTime();
		user.create(userUid, defaultUser(userUid));

		IUserMailIdentities identityService = getIdentityService(userUid);
		String identityUid = "john-id";
		UserMailIdentity userMailIdentity = createIdentity(userUid);
		identityService.create(identityUid, userMailIdentity);

		UserMailIdentity identity = identityService.get(identityUid);
		identity.name = "John Doe Updated";
		identityService.update(identityUid, identity);
		identityService.delete(identityUid);

		assertTrue(IdentityHook.latch.await(15, TimeUnit.SECONDS));
	}

	private UserMailIdentity createIdentity(String userUid) {
		UserMailIdentity userMailIdentity = new UserMailIdentity();
		userMailIdentity.displayname = "John Doe";
		userMailIdentity.name = "John Doe";
		userMailIdentity.email = userUid + "@" + domainUid;
		userMailIdentity.format = SignatureFormat.HTML;
		userMailIdentity.signature = "-- gg";
		userMailIdentity.mailboxUid = userUid;
		userMailIdentity.sentFolder = "Sent";
		userMailIdentity.isDefault = false;
		return userMailIdentity;
	}

	private User defaultUser(String login) {
		User user = new User();
		user.login = login;
		Email em = new Email();
		em.address = login + "@" + domainUid;
		em.isDefault = true;
		em.allAliases = false;
		user.emails = Arrays.asList(em);
		user.password = "password";
		user.routing = Mailbox.Routing.none;
		user.dataLocation = PopulateHelper.FAKE_CYRUS_IP;

		VCard card = new VCard();
		card.identification.name = Name.create("Doe", "John", null, null, null, null);
		user.contactInfos = card;
		return user;
	}

	private IUser getUserService() {
		return ServerSideServiceProvider.getProvider(ctx).instance(IUser.class, domainUid);
	}

	private IUserMailIdentities getIdentityService(String uid) {
		return ServerSideServiceProvider.getProvider(ctx).instance(IUserMailIdentities.class, domainUid, uid);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

}
