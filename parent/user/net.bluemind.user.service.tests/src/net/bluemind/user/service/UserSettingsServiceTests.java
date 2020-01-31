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

import com.google.common.collect.Lists;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.service.DomainsContainerIdentifier;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUserSettings;
import net.bluemind.user.persistence.UserSettingsStore;

public class UserSettingsServiceTests {

	private SecurityContext adminSecurityContext;

	private String user1;
	private SecurityContext user1SecurityContext;

	private String user2;

	protected String testDom;

	private IDomainSettings domainSettingsApi;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		ContainerStore containerHome = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM);

		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});
		launched.await();

		initDomainAndDomainSettings(containerHome);

	}

	private void initDomainAndDomainSettings(ContainerStore containerHome) throws Exception {

		testDom = "dom." + System.nanoTime() + ".lan";

		String cyrusIp = new BmConfIni().get("imap-role");
		Server imapServer = new Server();
		imapServer.ip = cyrusIp;
		imapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(imapServer);
		PopulateHelper.createTestDomain(testDom, imapServer);

		// create domain parititon on cyrus
		new CyrusService(cyrusIp).createPartition(testDom);
		new CyrusService(cyrusIp).refreshPartitions(Arrays.asList(testDom));
		new CyrusService(cyrusIp).reload();

		Container domains = containerHome.get(DomainsContainerIdentifier.getIdentifier());
		assertNotNull(domains);

		String nt = "" + System.nanoTime();
		String adm = "adm" + nt;
		adminSecurityContext = BmTestContext
				.contextWithSession(adm, adm + "@" + testDom, testDom, BasicRoles.ROLE_MANAGE_USER)
				.getSecurityContext();

		String u1 = "u1." + nt;
		user1 = PopulateHelper.addUserWithRoles(u1, testDom, BasicRoles.ROLE_SELF_CHANGE_SETTINGS);

		user1SecurityContext = BmTestContext
				.contextWithSession(u1, user1, testDom, BasicRoles.ROLE_SELF_CHANGE_SETTINGS).getSecurityContext();

		String u2 = "u2." + nt;
		user2 = PopulateHelper.addUserWithRoles(u2, testDom, BasicRoles.ROLE_SELF_CHANGE_SETTINGS);

		this.domainSettingsApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, testDom);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected IUserSettings getSettingsService(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(IUserSettings.class, testDom);
	}

	@Test
	public void testCreate() throws ServerFault, InterruptedException, SQLException {
		Map<String, String> us = getSettingsService(user1SecurityContext).get(user1);
		assertNotNull(us);
		assertTrue(us.size() > 0);
		assertEquals("18", us.get("work_hours_end"));
		assertEquals("fr", us.get("lang"));
	}

	@Test
	public void testDomainSettings() throws ServerFault, InterruptedException, SQLException {
		HashMap<String, String> domainSettings = new HashMap<String, String>();
		domainSettings.put("lang", "en");
		domainSettingsApi.set(domainSettings);

		Map<String, String> us = getSettingsService(user1SecurityContext).get(user1);
		assertNotNull(us);
		assertTrue(us.size() > 0);
		assertEquals("18", us.get("work_hours_end"));
		assertEquals("en", us.get("lang"));
	}

	@Test
	public void testUserSettings() throws ServerFault, InterruptedException, SQLException {
		HashMap<String, String> userSettings = new HashMap<String, String>();
		userSettings.put("lang", "en");

		getSettingsService(user1SecurityContext).set(user1, userSettings);

		Map<String, String> us = getSettingsService(user1SecurityContext).get(user1);
		assertNotNull(us);
		assertTrue(us.size() > 0);
		assertEquals("18", us.get("work_hours_end"));
		assertEquals("en", us.get("lang"));
	}

	@Test
	public void testUserDomainSettings() throws ServerFault, InterruptedException, SQLException {
		HashMap<String, String> domainSettings = new HashMap<String, String>();
		domainSettings.put("lang", "uk");
		domainSettings.put("work_hours_end", "15");
		domainSettingsApi.set(domainSettings);

		HashMap<String, String> userSettings = new HashMap<String, String>();
		userSettings.put("lang", "en");

		getSettingsService(user1SecurityContext).set(user1, userSettings);

		Map<String, String> us = getSettingsService(user1SecurityContext).get(user1);
		assertNotNull(us);
		assertTrue(us.size() > 0);
		assertEquals("15", us.get("work_hours_end"));
		assertEquals("en", us.get("lang"));
	}

	@Test
	public void testU1GetU2Settings() throws ServerFault, InterruptedException, SQLException {
		try {
			getSettingsService(user1SecurityContext).get(user2);
			fail("U1 must not access U2 settings !");
		} catch (ServerFault af) {
			assertEquals(ErrorCode.PERMISSION_DENIED, af.getCode());
		}
	}

	@Test
	public void user1SetUser2Settings() throws ServerFault, InterruptedException, SQLException {
		try {
			HashMap<String, String> userSettings = new HashMap<String, String>();
			userSettings.put("lang", "en");

			getSettingsService(user1SecurityContext).set(user2, userSettings);
			fail("U1 must not access U2 settings !");
		} catch (ServerFault af) {
			assertEquals(ErrorCode.PERMISSION_DENIED, af.getCode());
		}
	}

	@Test
	public void adminGetUserSettings() throws ServerFault, InterruptedException, SQLException {
		Map<String, String> us = getSettingsService(adminSecurityContext).get(user1);
		assertNotNull(us);
		assertTrue(us.size() > 0);
		assertEquals("18", us.get("work_hours_end"));
		assertEquals("fr", us.get("lang"));
	}

	@Test
	public void adminSetUserSettings() throws ServerFault, InterruptedException, SQLException {
		HashMap<String, String> userSettings = new HashMap<String, String>();
		userSettings.put("lang", "en");

		getSettingsService(adminSecurityContext).set(user1, userSettings);

		Map<String, String> us = getSettingsService(user1SecurityContext).get(user1);
		assertNotNull(us);
		assertTrue(us.size() > 0);
		assertEquals("18", us.get("work_hours_end"));
		assertEquals("en", us.get("lang"));
	}

	@Test
	public void domainSettingsDontDescendToUserSettings() throws ServerFault, InterruptedException, SQLException {
		HashMap<String, String> userSettings = new HashMap<String, String>();
		userSettings.put("lang", "en");
		userSettings.put("work_hours_end", "12");

		HashMap<String, String> duplicatedSettings = new HashMap<String, String>();
		duplicatedSettings.put("key1", "value1");
		duplicatedSettings.put("key2", "value2");

		userSettings.putAll(duplicatedSettings);
		domainSettingsApi.set(duplicatedSettings);

		ContainerStore containerStore = new ContainerStore(new BmTestContext(SecurityContext.SYSTEM),
				JdbcActivator.getInstance().getDataSource(), SecurityContext.SYSTEM);

		getSettingsService(adminSecurityContext).set(user1, userSettings);
		Container userSettingsContainer = containerStore.get(testDom);
		UserSettingsStore userSettingsStore = new UserSettingsStore(JdbcActivator.getInstance().getDataSource(),
				userSettingsContainer);

		ContainerStoreService<Map<String, String>> userSettingsStoreService = new ContainerStoreService<>(
				JdbcActivator.getInstance().getDataSource(), SecurityContext.SYSTEM, userSettingsContainer,
				"usersettings", userSettingsStore);

		Map<String, String> us = userSettingsStoreService.get(user1, null).value;

		assertEquals("12", us.get("work_hours_end"));
		assertEquals("en", us.get("lang"));
		assertEquals(2, us.size());
	}
	
	@Test
	public void setAndGetUserSettingOneByOne() {
		System.err.println(getSettingsService(adminSecurityContext).get(user1));
		
		String name = "myName";
		String value = "myValue";
		getSettingsService(adminSecurityContext).setOne(user1, name, value);
		assertEquals(value, getSettingsService(adminSecurityContext).getOne(user1, name));
		
		String secondName = "secondName";
		String secondValue = "secondValue";
		getSettingsService(adminSecurityContext).setOne(user1, secondName, secondValue);
		assertEquals(secondValue, getSettingsService(adminSecurityContext).getOne(user1, secondName));
		assertEquals(value, getSettingsService(adminSecurityContext).getOne(user1, name));
	}
}
