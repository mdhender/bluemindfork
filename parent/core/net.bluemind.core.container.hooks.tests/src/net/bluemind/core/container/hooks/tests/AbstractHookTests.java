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

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Identification.Name;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public abstract class AbstractHookTests {

	protected SecurityContext context;
	protected String domainUid = "dom" + System.currentTimeMillis() + ".lan";

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		context = BmTestContext.contextWithSession("admin", "admin", domainUid, SecurityContext.ROLE_ADMIN)
				.getSecurityContext();

		PopulateHelper.initGlobalVirt();

		PopulateHelper.createTestDomain(domainUid);
		PopulateHelper.addDomainAdmin("admin", domainUid);
		PopulateHelper.domainAdmin(domainUid, context.getSubject());

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected void defaultUser(String uid, String login) throws ServerFault {

		User user = new User();
		user.login = login;
		Email em = new Email();
		em.address = login + "@" + domainUid;
		em.isDefault = true;
		em.allAliases = false;
		user.emails = Arrays.asList(em);
		user.password = "password";
		user.routing = Routing.internal;
		user.dataLocation = PopulateHelper.FAKE_CYRUS_IP;

		VCard card = new VCard();
		card.identification.name = Name.create("Doe", "John", null, null, null, null);
		user.contactInfos = card;

		IUser userService = ServerSideServiceProvider.getProvider(context).instance(IUser.class, domainUid);

		userService.create(uid, user);
	}
}
