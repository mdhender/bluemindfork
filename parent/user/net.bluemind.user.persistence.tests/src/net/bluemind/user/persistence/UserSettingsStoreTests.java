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
package net.bluemind.user.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.Email;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.user.api.User;
import net.bluemind.user.api.UserSettings;

public class UserSettingsStoreTests {
	private static Logger logger = LoggerFactory.getLogger(UserSettingsStoreTests.class);
	private UserStore userStore;
	private ItemStore domainItemStore;
	private String uid;
	private Container installation;
	private UserSettingsStore userSettingsStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerStore = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				securityContext);
		String containerId = "test_" + System.nanoTime() + ".fr";
		Container domain = Container.create(containerId, "domain", containerId, "me", true);
		domain = containerStore.create(domain);

		this.uid = "test_" + System.nanoTime();

		assertNotNull(domain);

		domainItemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), domain, securityContext);

		installation = Container.create(InstallationId.getIdentifier(), "installation", "installation", "me", true);
		installation = containerStore.create(installation);
		AclStore aclStore = new AclStore(JdbcTestHelper.getInstance().getDataSource());
		aclStore.store(installation, Arrays.asList(AccessControlEntry.create(securityContext.getSubject(), Verb.All)));

		userStore = new UserStore(JdbcTestHelper.getInstance().getDataSource(), domain);

		userSettingsStore = new UserSettingsStore(JdbcTestHelper.getInstance().getDataSource(), domain);

		logger.debug("stores: {} {} {}", domainItemStore, userStore, userSettingsStore);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testNoUserSettings() throws Exception {
		domainItemStore.create(Item.create(uid, null));
		Item item = domainItemStore.get(uid);
		User u = getDefaultUser();
		userStore.create(item, u);

		// Test get by item ID
		UserSettings userSettings = userSettingsStore.get(item);
		assertNull(userSettings);
	}

	@Test
	public void testCreate() throws Exception {
		domainItemStore.create(Item.create(uid, null));
		Item item = domainItemStore.get(uid);
		User u = getDefaultUser();
		userStore.create(item, u);

		// Test create settings
		HashMap<String, String> userSettings = new HashMap<>();
		userSettings.put("lang", "fr");
		userSettingsStore.create(item, UserSettings.of(userSettings));

		UserSettings loadedUserSettings = userSettingsStore.get(item);
		assertNotNull(loadedUserSettings);
		assertNotNull(loadedUserSettings.values);
	}

	@Test
	public void testGetById() throws Exception {
		domainItemStore.create(Item.create(uid, null));
		Item item = domainItemStore.get(uid);
		User u = getDefaultUser();
		userStore.create(item, u);

		// Test create settings
		HashMap<String, String> userSettings = new HashMap<>();
		userSettings.put("lang", "fr");
		userSettingsStore.create(item, UserSettings.of(userSettings));

		// Test get settings by item ID
		UserSettings loadedUserSettings = userSettingsStore.get(item);
		assertNotNull(loadedUserSettings);
		assertEquals(userSettings.size(), loadedUserSettings.values.size());
		assertEquals(userSettings.get("lang"), loadedUserSettings.values.get("lang"));
	}

	@Test
	public void testUpdate() throws Exception {
		domainItemStore.create(Item.create(uid, null));
		Item item = domainItemStore.get(uid);
		User u = getDefaultUser();
		userStore.create(item, u);

		// Test create settings
		HashMap<String, String> userSettings = new HashMap<String, String>();
		userSettings.put("lang", "fr");
		userSettingsStore.create(item, UserSettings.of(userSettings));

		// Test get settings by item ID
		UserSettings loadedUserSettings = userSettingsStore.get(item);
		assertNotNull(loadedUserSettings);
		assertNotNull(loadedUserSettings.values);

		// Test update settings
		userSettings.put("lang", "en");
		userSettings.put("work_hours_end", "15");
		userSettingsStore.update(item, UserSettings.of(userSettings));

		// Test get settings by item ID
		loadedUserSettings = userSettingsStore.get(item);
		assertNotNull(loadedUserSettings);
		assertNotNull(loadedUserSettings.values);
		assertEquals(userSettings.size(), loadedUserSettings.values.size());
		assertEquals(userSettings.get("lang"), loadedUserSettings.values.get("lang"));
		assertEquals(userSettings.get("work_hours_end"), loadedUserSettings.values.get("work_hours_end"));
	}

	@Test
	public void testDelete() throws Exception {
		domainItemStore.create(Item.create(uid, null));
		Item item = domainItemStore.get(uid);
		User u = getDefaultUser();
		userStore.create(item, u);

		// Test create settings
		HashMap<String, String> userSettings = new HashMap<>();
		userSettings.put("lang", "fr");
		userSettingsStore.create(item, UserSettings.of(userSettings));

		// Test delete settings
		userSettingsStore.delete(item);
		UserSettings loadedUserSettings = userSettingsStore.get(item);
		assertNull(loadedUserSettings);
	}

	@Test
	public void testDeleteAll() throws Exception {
		domainItemStore.create(Item.create(uid, null));
		Item item = domainItemStore.get(uid);
		User u = getDefaultUser();
		userStore.create(item, u);

		String uid2 = "test_" + System.nanoTime();
		domainItemStore.create(Item.create(uid2, null));
		Item item2 = domainItemStore.get(uid2);
		User u2 = getDefaultUser();
		userStore.create(item2, u2);

		HashMap<String, String> userSettings = new HashMap<>();
		userSettings.put("lang", "fr");

		userSettingsStore.create(item, UserSettings.of(userSettings));
		UserSettings us = userSettingsStore.get(item);
		assertNotNull(us);
		assertNotNull(us.values);

		userSettingsStore.create(item2, UserSettings.of(userSettings));
		us = userSettingsStore.get(item2);
		assertNotNull(us);

		// Test delete all settings
		userSettingsStore.deleteAll();

		us = userSettingsStore.get(item);
		assertNull(us);
	}

	private User getDefaultUser() {
		User u = new User();
		u.login = "test" + System.nanoTime();
		u.password = "password";
		u.routing = Routing.none;
		u.archived = false;
		u.hidden = false;
		u.system = false;
		Email e = new Email();
		e.address = u.login + "@blue-mind.loc";
		u.emails = Arrays.asList(e);
		u.dataLocation = null;
		return u;
	}

}
