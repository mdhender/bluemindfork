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
package net.bluemind.locator.client.tests;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.collect.Lists;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.container.persistance.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.persistance.DirEntryStore;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.locator.impl.LocatorDbHelper;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.User;

public class LocatorDbHelperTests {

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		Server imapServer = new Server();
		imapServer.ip = "1.1.1.1";
		imapServer.tags = Lists.newArrayList("mail/imap");

		Server imapServer2 = new Server();
		imapServer2.ip = "2.2.2.2";
		imapServer2.tags = Lists.newArrayList("mail/imap");

		JdbcActivator.getInstance().addMailboxDataSource(imapServer.ip,
				JdbcTestHelper.getInstance().getMailboxDataDataSource());
		JdbcActivator.getInstance().addMailboxDataSource(imapServer2.ip,
				JdbcTestHelper.getInstance().getMailboxDataDataSource());

		PopulateHelper.initGlobalVirt(imapServer, imapServer2);
		PopulateHelper.createTestDomain("bm.lan", imapServer, imapServer2);
		User user = PopulateHelper.getUser("test", "bm.lan", Routing.none);
		user.dataLocation = imapServer.ip;
		PopulateHelper.addUser("bm.lan", user);

		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});
		launched.await();
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testLocator() throws SQLException {
		Set<String> dataLoc = LocatorDbHelper.findUserAssignedHosts("test@bm.lan", "mail/imap");
		assertEquals(1, dataLoc.size());
		assertEquals("1.1.1.1", dataLoc.iterator().next());

		// update dataloc
		ContainerStore cs = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM);
		Container dir = cs.get("bm.lan");
		DirEntryStore dirEntryStore = new DirEntryStore(JdbcTestHelper.getInstance().getDataSource(), dir);
		ItemStore itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), dir, SecurityContext.SYSTEM);
		Item item = itemStore.get("test");
		DirEntry de = dirEntryStore.get(item);
		de.dataLocation = "2.2.2.2";
		dirEntryStore.update(item, de);

		// fetch new dataloc
		dataLoc = LocatorDbHelper.findUserAssignedHosts("test@bm.lan", "mail/imap");
		assertEquals(1, dataLoc.size());
		assertEquals("2.2.2.2", dataLoc.iterator().next());
	}
}
