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
package net.bluemind.core.container.persistance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerSyncStatus;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;

public class ContainersSyncStoreTests {

	private ContainerStore cs;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		SecurityContext securityContext = new SecurityContext(null, "test", Arrays.<String>asList("groupOfUsers"),
				Arrays.<String>asList(), "fakeDomain");

		cs = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(), securityContext);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testUpdateWithoutInit() throws Exception {
		Container c1 = cs.create(Container.create(UUID.randomUUID().toString(), "calendar", "c1", ""));
		ContainerSyncStore css = new ContainerSyncStore(JdbcTestHelper.getInstance().getDataSource(), c1);

		// no init
		// css.initSync();
		ContainerSyncStatus ss = new ContainerSyncStatus();
		ss.syncToken = "token";
		Calendar ns1 = Calendar.getInstance();
		ns1.set(2016, 1, 13, 2, 0, 0);
		ss.nextSync = ns1.getTimeInMillis();
		css.setSyncStatus(ss);

		ContainerSyncStatus us = css.getSyncStatus();
		assertEquals(ss.syncToken, us.syncToken);
	}

	@Test
	public void testInitShouldSetInititalSync() throws Exception {
		Container c1 = cs.create(Container.create(UUID.randomUUID().toString(), "calendar", "c1", ""));
		ContainerSyncStore css = new ContainerSyncStore(JdbcTestHelper.getInstance().getDataSource(), c1);

		css.initSync();
		ContainerSyncStatus us = css.getSyncStatus();
		assertNotNull(us.nextSync);
		assertTrue(us.nextSync < System.currentTimeMillis());
	}

	@Test
	public void list() throws Exception {
		ContainersSyncStore store = new ContainersSyncStore(JdbcTestHelper.getInstance().getDataSource());
		ContainerSyncStatus ss = new ContainerSyncStatus();
		ss.syncToken = "token";
		Calendar ns1 = Calendar.getInstance();
		ns1.set(2016, 1, 13, 2, 0, 0);
		ss.nextSync = ns1.getTimeInMillis();

		Container c1 = cs.create(Container.create(UUID.randomUUID().toString(), "calendar", "c1", ""));
		ContainerSyncStore css = new ContainerSyncStore(JdbcTestHelper.getInstance().getDataSource(), c1);
		css.initSync();
		css.setSyncStatus(ss);

		Map<String, String> settings = new HashMap<>();
		settings.put("icsUrl", "http://somewhere");

		new ContainerSettingsStore(JdbcTestHelper.getInstance().getDataSource(), c1).setSettings(settings);

		Calendar ns2 = Calendar.getInstance();
		ns2.set(2022, 1, 13, 2, 0, 0);
		ss.nextSync = ns2.getTimeInMillis();

		Container c2 = cs.create(Container.create(UUID.randomUUID().toString(), "calendar", "c2", ""));
		css = new ContainerSyncStore(JdbcTestHelper.getInstance().getDataSource(), c2);
		css.initSync();
		css.setSyncStatus(ss);

		new ContainerSettingsStore(JdbcTestHelper.getInstance().getDataSource(), c2).setSettings(settings);

		Container a1 = cs.create(Container.create(UUID.randomUUID().toString(), "addressbook", "a1", ""));
		css = new ContainerSyncStore(JdbcTestHelper.getInstance().getDataSource(), a1);
		css.initSync();
		css.setSyncStatus(ss);

		new ContainerSettingsStore(JdbcTestHelper.getInstance().getDataSource(), a1).setSettings(settings);

		List<String> res = store.list("calendar", ns2.getTimeInMillis(), 10, "icsUrl");
		assertEquals(2, res.size());
		assertEquals(c1.uid, res.get(0));
		assertEquals(c2.uid, res.get(1));

		res = store.list("calendar", ns2.getTimeInMillis(), 1, "icsUrl");
		assertEquals(1, res.size());
		assertEquals(c1.uid, res.get(0));

		res = store.list("calendar", ns1.getTimeInMillis(), 10, "icsUrl");
		assertEquals(1, res.size());
		assertEquals(c1.uid, res.get(0));

		res = store.list("addressbook", ns1.getTimeInMillis(), 10, "icsUrl");
		assertEquals(0, res.size());

		res = store.list("addressbook", ns2.getTimeInMillis(), 10, "icsUrl");
		assertEquals(1, res.size());

		res = store.list("test", ns2.getTimeInMillis(), 10, "icsUrl");
		assertEquals(0, res.size());

	}

	/**
	 * When synchronizing a deleted {@link Container}, t_container_sync should be
	 * cleaned since it stores a foreign key.
	 */
	@Test
	public void testSyncDeletedContainerCleanDatabase() throws SQLException {
		// create a container
		final String containerUID = UUID.randomUUID().toString();
		final Container container = this.cs.create(Container.create(containerUID, "calendar", "c1", ""));

		// initialize synchronization mechanism
		final ContainerSyncStore containerSyncStore = new ContainerSyncStore(
				JdbcTestHelper.getInstance().getDataSource(), container);
		containerSyncStore.initSync();
		final ContainerSyncStatus containerSyncStatus = new ContainerSyncStatus();
		containerSyncStatus.syncToken = "token";
		final Calendar calendar = Calendar.getInstance();
		calendar.set(2016, 1, 13, 2, 0, 0);
		containerSyncStatus.nextSync = calendar.getTimeInMillis();

		// request a synchronization
		containerSyncStore.setSyncStatus(containerSyncStatus);

		// check sync status exists
		Assert.assertNotNull("Sync status should exist", containerSyncStore.getSyncStatus());

		// delete the container
		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IContainers.class).delete(container.uid);

		// request a synchronization (again)
		calendar.set(2016, 1, 13, 3, 0, 0);
		containerSyncStatus.nextSync = calendar.getTimeInMillis();
		containerSyncStore.setSyncStatus(containerSyncStatus);

		// check sync status does not exist
		Assert.assertNull("Sync status should not exist", containerSyncStore.getSyncStatus());
	}

}
