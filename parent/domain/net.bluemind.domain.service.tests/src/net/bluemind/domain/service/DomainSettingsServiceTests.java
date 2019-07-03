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
package net.bluemind.domain.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.collect.ImmutableMap;

import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.system.api.IGlobalSettings;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.User;
import net.bluemind.user.service.internal.ContainerUserStoreService;

public class DomainSettingsServiceTests {
	private User admin;

	private User user1;

	private SecurityContext adminSecurityContext;
	private SecurityContext userSecurityContext;
	protected String testDomainUid;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		testDomainUid = "test.lan";

		
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		PopulateHelper.initGlobalVirt();
		ItemValue<Domain> domain = PopulateHelper.createTestDomain(testDomainUid);

		BmTestContext adminBmContext = BmTestContext.contextWithSession("adminSessionId", "testAdmin", testDomainUid,
				BasicRoles.ROLE_MANAGE_DOMAIN);
		adminSecurityContext = adminBmContext.getSecurityContext();

		ContainerStore containerHome = new ContainerStore(adminBmContext, JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM);

		initUserStore(containerHome, domain);

		userSecurityContext = BmTestContext.contextWithSession("userSessionId", "testUser", testDomainUid)
				.getSecurityContext();

		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});
		launched.await();

		// initiliaze global settings
		IGlobalSettings globalSettingsApi = ServerSideServiceProvider.getProvider(adminBmContext)
				.instance(IGlobalSettings.class);
		globalSettingsApi.set(ImmutableMap.<String, String>builder().put("test", "1").build());
	}

	private void initUserStore(ContainerStore containerHome, ItemValue<Domain> domain)
			throws SQLException, ServerFault {
		Container usersContainer = containerHome.get(testDomainUid);

		ContainerUserStoreService userStoreService = new ContainerUserStoreService(
				new BmTestContext(SecurityContext.SYSTEM), usersContainer, domain);

		admin = defaultUser("testAdmin");
		userStoreService.create("testAdmin", admin);

		user1 = defaultUser("testUser");
		userStoreService.create("testUser", user1);
	}

	private User defaultUser(String login) {
		User user = new User();
		user.login = login;
		Email em = new Email();
		em.address = login + "@" + testDomainUid;
		em.isDefault = true;
		em.allAliases = false;
		user.emails = Arrays.asList(em);
		user.password = "password";
		user.routing = Mailbox.Routing.none;
		return user;
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected IDomainSettings getSettingsService(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(IDomainSettings.class, testDomainUid);

	}

	@Test
	public void testCreate() throws ServerFault, InterruptedException, SQLException {
		Map<String, String> us = getSettingsService(adminSecurityContext).get();
		assertNotNull(us);
		assertTrue(us.size() > 0);
		assertEquals("1", us.get("test"));
	}

	@Test
	public void testDomainSettings() throws ServerFault, InterruptedException, SQLException {
		HashMap<String, String> domainSettings = new HashMap<String, String>();
		domainSettings.put("lang", "en");
		getSettingsService(adminSecurityContext).set(domainSettings);

		Map<String, String> us = getSettingsService(adminSecurityContext).get();
		assertNotNull(us);
		assertTrue(us.size() > 0);
		assertEquals("1", us.get("test"));
	}

	@Test
	public void testU1GetDomainSettings() throws ServerFault, InterruptedException, SQLException {
		try {
			getSettingsService(userSecurityContext).get();
			fail("U1 must not access domain settings !");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void testU1SetDomainSettings() throws ServerFault, InterruptedException, SQLException {
		try {
			HashMap<String, String> domainSettings = new HashMap<String, String>();
			domainSettings.put("lang", "en");
			getSettingsService(userSecurityContext).set(domainSettings);
			fail("U1 must not update domain settings !");
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}
}
