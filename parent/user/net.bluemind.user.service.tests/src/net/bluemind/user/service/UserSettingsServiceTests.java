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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
import net.bluemind.role.api.BasicRoles;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUserSettings;
import net.bluemind.user.api.UserSettings;
import net.bluemind.user.persistence.UserSettingsStore;
import net.bluemind.user.service.internal.UserSettingsTestHook;

public class UserSettingsServiceTests {

	private SecurityContext adminSecurityContext;

	private String user1;
	private SecurityContext user1SecurityContext;

	private String user2;

	protected String testDom;

	private IDomainSettings domainSettingsApi;

	@Before
	public void before() throws Exception {
		System.setProperty("imap.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);

		JdbcTestHelper.getInstance().beforeTest();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		ContainerStore containerHome = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM);

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		initDomainAndDomainSettings(containerHome);

	}

	private void initDomainAndDomainSettings(ContainerStore containerHome) throws Exception {

		testDom = "dom." + System.nanoTime() + ".lan";

		Server pipo = new Server();
		pipo.tags = Collections.singletonList("mail/imap");
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;

		PopulateHelper.initGlobalVirt(pipo);
		PopulateHelper.createTestDomain(testDom, pipo);

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
		assertTrue(UserSettingsTestHook.called());

		Map<String, String> us = getSettingsService(user1SecurityContext).get(user1);
		assertNotNull(us);
		assertTrue(us.size() > 0);
		assertEquals("18", us.get("work_hours_end"));
		assertEquals("en", us.get("lang"));
	}

	@Test
	public void testUserDomainSettings() throws ServerFault, InterruptedException, SQLException {
		HashMap<String, String> domainSettings = new HashMap<String, String>();
		domainSettings.put("lang", "en");
		domainSettings.put("work_hours_end", "15");
		domainSettingsApi.set(domainSettings);

		HashMap<String, String> userSettings = new HashMap<String, String>();
		userSettings.put("lang", "en");

		getSettingsService(user1SecurityContext).set(user1, userSettings);
		assertTrue(UserSettingsTestHook.called());

		Map<String, String> us = getSettingsService(user1SecurityContext).get(user1);
		assertNotNull(us);
		assertTrue(us.size() > 0);
		assertEquals("15", us.get("work_hours_end"));
		assertEquals("en", us.get("lang"));
	}

	@Test
	public void testUserDomainSettingsMustFail() throws ServerFault, InterruptedException, SQLException {
		HashMap<String, String> domainSettings = new HashMap<String, String>();
		domainSettings.put("lang", "uk");
		domainSettings.put("work_hours_end", "15");
		ServerFault thrown = assertThrows(ServerFault.class, () -> domainSettingsApi.set(domainSettings));
		assertTrue(thrown.getMessage().contentEquals("Languages uk is not supported"));
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
		assertTrue(UserSettingsTestHook.called());

		Map<String, String> us = getSettingsService(user1SecurityContext).get(user1);
		assertNotNull(us);
		assertTrue(us.size() > 0);
		assertEquals("18", us.get("work_hours_end"));
		assertEquals("en", us.get("lang"));
	}

	@Test
	public void domainSettingsDontDescendToUserSettings() throws ServerFault, InterruptedException, SQLException {
		HashMap<String, String> userSettings = new HashMap<>();
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

		ContainerStoreService<UserSettings> userSettingsStoreService = new ContainerStoreService<>(
				JdbcActivator.getInstance().getDataSource(), SecurityContext.SYSTEM, userSettingsContainer,
				userSettingsStore);

		Map<String, String> us = userSettingsStoreService.get(user1, null).value.values;

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
		assertTrue(UserSettingsTestHook.called());
		assertEquals(value, getSettingsService(adminSecurityContext).getOne(user1, name));

		String secondName = "secondName";
		String secondValue = "secondValue";
		getSettingsService(adminSecurityContext).setOne(user1, secondName, secondValue);
		assertEquals(secondValue, getSettingsService(adminSecurityContext).getOne(user1, secondName));
		assertEquals(value, getSettingsService(adminSecurityContext).getOne(user1, name));
	}

	@After
	public void tearDown() {
		UserSettingsTestHook.reset();
	}
}
