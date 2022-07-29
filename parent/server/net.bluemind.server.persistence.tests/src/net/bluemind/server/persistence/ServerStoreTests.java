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

package net.bluemind.server.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.nio.charset.Charset;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.io.Files;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.server.api.Server;

public class ServerStoreTests {

	private ServerStore serverStore;
	private String uid;
	private ItemStore installationItemStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		
		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerStore = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				securityContext);
		String mcastId = "fake";

		if (new File("/etc/bm/mcast.id").exists()) {
			mcastId = Files.toString(new File("/etc/bm/mcast.id"), Charset.defaultCharset());
		}
		String containerId = "bluemind-" + mcastId;
		Container installation = Container.create(containerId, "installation", containerId, "me", true);
		installation = containerStore.create(installation);
		installationItemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), installation,
				securityContext);
		serverStore = new ServerStore(JdbcTestHelper.getInstance().getDataSource(), installation);
		uid = "server-" + UUID.randomUUID().toString();
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCreateGetUpdateDelete() throws Exception {
		installationItemStore.create(Item.create(uid, null));
		Item item = installationItemStore.get(uid);
		Server u = getDefaultServer();
		serverStore.create(item, u);

		Server created = serverStore.get(item);
		assertNotNull("Nothing found", created);
		assertNotNull(created);
		assertNotNull(created.fqdn);
		assertNotNull(created.ip);
		assertNotNull(created.tags);
		created.fqdn = "updated_" + System.nanoTime();
		serverStore.update(item, created);

		Server found = serverStore.get(item);
		assertNotNull(found);
		assertEquals(created.fqdn, found.fqdn);

		serverStore.assign(uid, "toto.fr", "mail/imap");

		serverStore.delete(item);
		found = serverStore.get(item);
		assertNull(found);
	}

	private Server getDefaultServer() {
		Server u = new Server();
		u.fqdn = uid + ".bm.lan";
		u.ip = "127.0.0.1";
		u.tags = Lists.newArrayList("blue/job", "john/bang");
		return u;
	}

}
