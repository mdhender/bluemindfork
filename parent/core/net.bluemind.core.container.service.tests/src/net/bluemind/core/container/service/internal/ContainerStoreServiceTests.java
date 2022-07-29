/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.core.container.service.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.EnumSet;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.model.ChangeLogEntry;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ChangelogStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.IItemValueStore;
import net.bluemind.core.container.service.internal.ContainerStoreService.IItemFlagsProvider;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.system.pg.PostgreSQLService;

public class ContainerStoreServiceTests {

	private SecurityContext securityContext;
	private Container container;
	private DataSource pool;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		securityContext = SecurityContext.ANONYMOUS;
		ContainerStore containerHome = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				securityContext);

		String containerId = "test_" + System.nanoTime();
		container = Container.create(containerId, "test", "test", "me", true);
		container = containerHome.create(container);
		assertNotNull(container);
		this.pool = JdbcTestHelper.getInstance().getDataSource();
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	public static final IItemFlagsProvider<Dummy> DUMMY_FLAGS = (d) -> {
		EnumSet<ItemFlag> ret = EnumSet.noneOf(ItemFlag.class);
		if (d.seen) {
			ret.add(ItemFlag.Seen);
		}
		if (d.deleted) {
			ret.add(ItemFlag.Deleted);
		}
		return ret;
	};

	@Test
	public void testXfer() throws SQLException {
		BmConfIni conf = new BmConfIni();
		String dbHost = conf.get("mailbox-role");

		initDataServer(dbHost);

		ContainerStoreService<Dummy> css = create(DUMMY_FLAGS);
		Dummy d = new Dummy();
		css.create("test", "test", d);
		css.update("test", "test-updated", d);
		css.update("test", "test-updated-one-more-time", d);

		css.create("test2", "test2", d);

		css.create("test3", "test3", d);
		css.delete("test3");

		css.create("test4", "test4", d);

		int origContainerSize = css.all().size();
		long origContainerVersion = css.getVersion();
		ChangelogStore changelogStore = new ChangelogStore(pool, container);
		ContainerChangelog origChangelog = changelogStore.changelog(0, Long.MAX_VALUE);
		int origChangelogEntries = origChangelog.entries.size();

		long test1Version = css.get("test", null).version;
		long test2Version = css.get("test2", null).version;
		long test4Version = css.get("test4", null).version;

		DataSource targetDs = JdbcActivator.getInstance().getMailboxDataSource(dbHost);

		ContainerStore containerHome = new ContainerStore(null, targetDs, securityContext);

		String containerId = "test_" + System.nanoTime();
		Container targetContainer = Container.create(containerId, "test", "test", "me", true);
		targetContainer = containerHome.create(container);

		IItemValueStore<Dummy> dstore = new DummyStore(targetContainer, targetDs);

		ContainerStoreService<Dummy> targetContainerStoreService = new ContainerStoreService<>(targetDs,
				SecurityContext.ANONYMOUS, targetContainer, dstore);

		assertTrue(targetContainerStoreService.getItemStore().all().isEmpty());

		css.xfer(targetDs, targetContainer, dstore);

		long newContainerVersion = targetContainerStoreService.getVersion();

		assertEquals(origContainerSize, targetContainerStoreService.getItemStore().all().size());
		assertEquals(3L, targetContainerStoreService.getVersion());

		ContainerChangelog changes = changelogStore.changelog(origContainerVersion,
				targetContainerStoreService.getVersion());
		assertTrue(changes.entries.isEmpty());

		changelogStore = new ChangelogStore(targetDs, targetContainer);

		// tranfered items version
		ItemValue<Dummy> test = targetContainerStoreService.get("test", null);
		assertNotNull(test);

		ItemValue<Dummy> test2 = targetContainerStoreService.get("test2", null);
		assertNotNull(test2);

		ItemValue<Dummy> test3 = targetContainerStoreService.get("test3", null);
		assertNull(test3);

		ItemValue<Dummy> test4 = targetContainerStoreService.get("test4", null);
		assertNotNull(test4);

		// new item creation is ok
		targetContainerStoreService.create("new", "new", d);
		assertEquals(newContainerVersion + 1, targetContainerStoreService.getVersion());
		changes = changelogStore.changelog(newContainerVersion, targetContainerStoreService.getVersion());
		assertEquals(1, changes.entries.size());
		assertEquals("new", changes.entries.get(0).itemUid);
		assertEquals(ChangeLogEntry.Type.Created, changes.entries.get(0).type);

		// current container is empty
		assertTrue(css.allUids().isEmpty());
		assertTrue(css.changelog(0L, Long.MAX_VALUE).entries.isEmpty());

	}

	private void initDataServer(String dbHost) {
		Server srv = new Server();
		srv.fqdn = dbHost;
		srv.ip = dbHost;
		srv.name = dbHost;
		srv.tags = Lists.newArrayList("bm/pgsql-data");

		ItemValue<Server> server = ItemValue.create(dbHost, srv);

		PostgreSQLService service = new TestPostgreSQLService();
		String dbName = "db-test-" + System.currentTimeMillis();
		service.addDataServer(server, dbName);
	}

	@Test
	public void testUnflagged() {
		crudOps((d) -> EnumSet.noneOf(ItemFlag.class));
	}

	@Test
	public void testFlagged() {
		crudOps(DUMMY_FLAGS);
	}

	@Test
	public void testFlaggedCounts() {
		ContainerStoreService<Dummy> css = create(DUMMY_FLAGS);
		Dummy d = new Dummy();
		css.create("test", "test", d);
		Count count = css.count(ItemFlagFilter.all());
		assertEquals(1, count.total);
		ItemValue<Dummy> value = css.get("test", null);
		assertTrue(value.flags.isEmpty());
		count = css.count(ItemFlagFilter.create().must(ItemFlag.Seen));
		assertEquals(0, count.total);

		d.seen = true;
		css.update("test", "test", d);

		count = css.count(ItemFlagFilter.create().must(ItemFlag.Seen));
		assertEquals(1, count.total);
		value = css.get("test", null);
		assertTrue(value.flags.contains(ItemFlag.Seen));
		count = css.count(ItemFlagFilter.create().must(ItemFlag.Seen).mustNot(ItemFlag.Deleted));
		assertEquals(1, count.total);

		d.deleted = true;
		css.update("test", "test", d);
		count = css.count(ItemFlagFilter.create().must(ItemFlag.Seen));
		assertEquals(1, count.total);
		count = css.count(ItemFlagFilter.create().must(ItemFlag.Seen).mustNot(ItemFlag.Deleted));
		assertEquals(0, count.total);
	}

	private ContainerStoreService<Dummy> create(IItemFlagsProvider<Dummy> fp) {
		return new ContainerStoreService<>(pool, SecurityContext.ANONYMOUS, container, new DummyStore(container, pool),
				fp, v -> 0L, s -> s);
	}

	public void testBrokenStore() {
		ContainerStoreService<Dummy> valid = create(DUMMY_FLAGS);
		ContainerStoreService<Dummy> throwing = createThrowing();
		try {
			Dummy d = new Dummy();
			throwing.create("test", "test", d);
			fail();
		} catch (ServerFault sf) {
			// ok
		}
		Dummy d = new Dummy();
		valid.create("test", "test", d);
		try {
			throwing.update("test", "test", d);
			fail();
		} catch (ServerFault sf) {
			// ok
		}
		try {
			throwing.delete("test");
			fail();
		} catch (ServerFault sf) {
			// ok
		}
		try {
			throwing.get("test", null);
			fail();
		} catch (ServerFault sf) {
			// ok
		}
	}

	private ContainerStoreService<Dummy> createThrowing() {
		return new ContainerStoreService<>(pool, SecurityContext.ANONYMOUS, container, new ThrowingStore(pool));
	}

	private void crudOps(IItemFlagsProvider<Dummy> fp) {
		ContainerStoreService<Dummy> css = create(fp);
		Dummy d = new Dummy();
		css.create("test", "test", d);
		d.seen = true;
		css.update("test", "test", d);
		assertEquals(1, css.all().size());
		ContainerChangeset<String> changes = css.changeset(0L, Long.MAX_VALUE);
		assertEquals(1, changes.created.size());

		css.delete("test");
		assertTrue(css.all().isEmpty());
	}

}
