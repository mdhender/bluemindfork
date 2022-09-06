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
package net.bluemind.authentication.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;

public class RoleValidationTests {

	private String domainUid1;
	private String domainUid2;

	@Before
	public void setup() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		JdbcTestHelper.getInstance().beforeTest();
		PopulateHelper.initGlobalVirt();
		IDomainSettings settings0 = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, "global.virt");
		Map<String, String> domainSettings0 = settings0.get();
		domainSettings0.put(DomainSettingsKeys.mail_routing_relay.name(), "external@test.fr");
		settings0.set(domainSettings0);
		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.external);

		domainUid1 = "1-" + System.currentTimeMillis() + ".loc";
		PopulateHelper.createDomain(domainUid1);
		PopulateHelper.addUser("user1", domainUid1);

		domainUid2 = "2-" + System.currentTimeMillis() + ".loc";
		PopulateHelper.createDomain(domainUid2);
		PopulateHelper.addUser("user2", domainUid2);

		PopulateHelper.createTestDomain("bm.lan");

	}

	@Test
	public void testRoleValidation() {
		IUser userService1 = getUserService(domainUid1);
		IUser userService2 = getUserService(domainUid2);

		userService1.setRoles("user1",
				new HashSet<>(Arrays.asList("ju-role1", "ju-role2", "ju-role3", "ju-role4", "ju-role5")));
		userService2.setRoles("user2",
				new HashSet<>(Arrays.asList("ju-role1", "ju-role2", "ju-role3", "ju-role4", "ju-role5")));

		IAuthentication authentication = getService();
		LoginResponse resp1 = authentication.login("user1@" + domainUid1, "user1", "junit");
		LoginResponse resp2 = authentication.login("user2@" + domainUid2, "user2", "junit");

		List<String> roles1 = Sessions.get().getIfPresent(resp1.authKey).getRoles();
		List<String> roles2 = Sessions.get().getIfPresent(resp2.authKey).getRoles();

		// no one owns role1
		assertFalse(roles1.contains("ju-role1"));
		assertFalse(roles2.contains("ju-role1"));

		// only domain1 owns role2
		assertTrue(roles1.contains("ju-role2"));
		assertFalse(roles2.contains("ju-role2"));

		// only domain2 owns role3
		assertFalse(roles1.contains("ju-role3"));
		assertTrue(roles2.contains("ju-role3"));

		// everybody owns role4
		assertTrue(roles1.contains("ju-role4"));
		assertTrue(roles2.contains("ju-role4"));

		// no one owns role5 (1 validator returns true, but another false)
		assertFalse(roles1.contains("ju-role5"));
		assertFalse(roles2.contains("ju-role5"));

	}

	private IAuthentication getService() throws ServerFault {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IAuthentication.class);
	}

	private IUser getUserService(String domain) throws ServerFault {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class, domain);
	}
}
