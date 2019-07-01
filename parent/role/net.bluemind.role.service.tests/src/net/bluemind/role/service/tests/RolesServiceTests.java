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
package net.bluemind.role.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.util.concurrent.SettableFuture;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.role.api.IRoles;
import net.bluemind.role.api.RoleDescriptor;
import net.bluemind.role.api.RolesCategory;
import net.bluemind.role.service.IInternalRoles;

public class RolesServiceTests {
	private SecurityContext enSC = new SecurityContext("slave", "slave", Arrays.<String>asList(),
			Arrays.asList("slave", "role1"), Collections.emptyMap(), "gg", "en", "role-service-tests-en");

	private SecurityContext frSC = new SecurityContext("simple", "simple", Arrays.<String>asList(),
			Arrays.asList("role1"), Collections.emptyMap(), "gg", "fr", "role-service-tests-en");

	@Before
	public void before() throws Exception {

		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();

		Sessions.get().put(enSC.getSessionId(), enSC);
		Sessions.get().put(frSC.getSessionId(), frSC);

	}

	@Test
	public void testGetRoles() throws ServerFault {
		Set<RoleDescriptor> roles = getRoles(enSC).getRoles();
		assertFalse(roles.isEmpty());

		boolean found = false;
		for (RoleDescriptor desc : roles) {
			if (desc.id.equals(TestRolesProvider.ROLE_TEST)) {
				found = true;
				assertEquals("label-en", desc.label);
				assertEquals("desc-en", desc.description);

			}
		}
		assertTrue(found);

		roles = getRoles(frSC).getRoles();
		assertFalse(roles.isEmpty());
		found = false;
		for (RoleDescriptor desc : roles) {
			if (desc.id.equals(TestRolesProvider.ROLE_TEST)) {
				found = true;
				assertEquals("label-fr", desc.label);
				assertEquals("desc-fr", desc.description);
			}
		}

		assertTrue(found);
	}

	@Test
	public void testGetRolesCategories() throws ServerFault {
		Set<RolesCategory> cats = getRoles(enSC).getRolesCategories();
		assertFalse(cats.isEmpty());

		boolean found = false;
		for (RolesCategory cat : cats) {
			if (cat.id.equals(TestRolesProvider.CATEGORY_TEST)) {
				found = true;
				assertEquals("cat-en", cat.label);
			}
		}
		assertTrue(found);

		cats = getRoles(frSC).getRolesCategories();
		assertFalse(cats.isEmpty());

		found = false;
		for (RolesCategory cat : cats) {
			if (cat.id.equals(TestRolesProvider.CATEGORY_TEST)) {
				found = true;
				assertEquals("cat-fr", cat.label);
			}
		}
		assertTrue(found);

	}

	@Test
	public void testDeactivateRoles() throws ServerFault {
		Set<String> roles = new HashSet<>();
		roles.add("role1");
		roles.add("role2");
		roles.add("role3");
		roles = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IInternalRoles.class)
				.filter(roles);

		assertEquals(1, roles.size());
		assertEquals("role2", roles.toArray(new String[0])[0]);
	}

	private IRoles getRoles(SecurityContext sc) throws ServerFault {
		return new BmTestContext(sc).provider().instance(IRoles.class);
	}
}
