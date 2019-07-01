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
package net.bluemind.domain.persistance.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.container.persistance.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.domain.persistance.DomainSettingsStore;

public class DomainSettingsStoreTests {
	private static Logger logger = LoggerFactory.getLogger(DomainSettingsStoreTests.class);
	private ItemStore domainItemStore;
	private String domainUid;
	private String domainUid2;
	private DomainSettingsStore domainSettingsStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		
		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerStore = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
				securityContext);

		String containerId = "test_" + System.nanoTime() + ".fr";
		Container domain = Container.create(containerId, "domain", containerId, "me", true);
		domain = containerStore.create(domain);
		assertNotNull(domain);

		domainUid = domain.uid;

		containerId = "test2_" + System.nanoTime() + ".fr";
		Container domain2 = Container.create(containerId, "domain", containerId, "me", true);
		domain2 = containerStore.create(domain2);
		assertNotNull(domain);

		domainUid2 = domain2.uid;

		domainItemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), domain, securityContext);

		domainSettingsStore = new DomainSettingsStore(JdbcTestHelper.getInstance().getDataSource(), domain);

		logger.debug("stores: {} {}", domainItemStore, domainSettingsStore);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testNoDomainSettings() throws Exception {
		domainItemStore.create(Item.create(domainUid, null));
		Item item = domainItemStore.get(domainUid);

		// Test get by item ID
		Map<String, String> domainSettings = domainSettingsStore.get(item);
		assertNull(domainSettings);
	}

	@Test
	public void testCreate() throws Exception {
		domainItemStore.create(Item.create(domainUid, null));
		Item item = domainItemStore.get(domainUid);

		// Test create settings
		HashMap<String, String> domainSettings = new HashMap<String, String>();
		domainSettings.put("lang", "fr");
		domainSettingsStore.create(item, domainSettings);

		Map<String, String> loadedDomainSettings = domainSettingsStore.get(item);
		assertNotNull(loadedDomainSettings);
	}

	@Test
	public void testGetById() throws Exception {
		domainItemStore.create(Item.create(domainUid, null));
		Item item = domainItemStore.get(domainUid);

		// Test create settings
		HashMap<String, String> domainSettings = new HashMap<String, String>();
		domainSettings.put("lang", "fr");
		domainSettingsStore.create(item, domainSettings);

		// Test get settings by item ID
		Map<String, String> loadedDomainSettings = domainSettingsStore.get(item);
		assertNotNull(loadedDomainSettings);
		assertEquals(domainSettings.size(), loadedDomainSettings.size());
		assertEquals(domainSettings.get("lang"), loadedDomainSettings.get("lang"));
	}

	@Test
	public void testUpdate() throws Exception {
		domainItemStore.create(Item.create(domainUid, null));
		Item item = domainItemStore.get(domainUid);

		// Test create settings
		HashMap<String, String> domainSettings = new HashMap<String, String>();
		domainSettings.put("lang", "fr");
		domainSettingsStore.create(item, domainSettings);

		// Test get settings by item ID
		Map<String, String> loadedDomainSettings = domainSettingsStore.get(item);
		assertNotNull(loadedDomainSettings);

		// Test update settings
		domainSettings.put("lang", "en");
		domainSettings.put("work_hours_end", "15");
		domainSettingsStore.update(item, domainSettings);

		// Test get settings by item ID
		loadedDomainSettings = domainSettingsStore.get(item);
		assertNotNull(loadedDomainSettings);
		assertEquals(domainSettings.size(), loadedDomainSettings.size());
		assertEquals(domainSettings.get("lang"), loadedDomainSettings.get("lang"));
		assertEquals(domainSettings.get("work_hours_end"), loadedDomainSettings.get("work_hours_end"));
	}

	@Test
	public void testDelete() throws Exception {
		domainItemStore.create(Item.create(domainUid, null));
		Item item = domainItemStore.get(domainUid);

		// Test create settings
		HashMap<String, String> domainSettings = new HashMap<String, String>();
		domainSettings.put("lang", "fr");
		domainSettingsStore.create(item, domainSettings);

		// Test delete settings
		domainSettingsStore.delete(item);
		Map<String, String> loadedDomainSettings = domainSettingsStore.get(item);
		assertNull(loadedDomainSettings);
	}

	@Test
	public void testDeleteAll() throws Exception {
		domainItemStore.create(Item.create(domainUid, null));
		Item item = domainItemStore.get(domainUid);

		domainItemStore.create(Item.create(domainUid2, null));
		Item item2 = domainItemStore.get(domainUid2);

		HashMap<String, String> domainSettings = new HashMap<String, String>();
		domainSettings.put("lang", "fr");

		domainSettingsStore.create(item, domainSettings);
		Map<String, String> us = domainSettingsStore.get(item);
		assertNotNull(us);

		domainSettingsStore.create(item2, domainSettings);
		us = domainSettingsStore.get(item2);
		assertNotNull(us);

		// Test delete all settings
		domainSettingsStore.deleteAll();

		us = domainSettingsStore.get(item);
		assertNull(us);
	}
}
