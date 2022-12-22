/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.smime.cacerts.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Random;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.smime.cacerts.api.SmimeCacert;

public class SmimeCacertStoreTests extends AbstractStoreTests {
	private static Logger logger = LoggerFactory.getLogger(SmimeCacertStoreTests.class);
	private SmimeCacertStore smimeStore;
	private ItemStore itemStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerHome = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				securityContext);
		String containerId = "test_" + System.nanoTime();
		Container container = Container.create(containerId, "test", "test", "me", true);
		container = containerHome.create(container);

		assertNotNull(container);

		itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), container, securityContext);

		smimeStore = new SmimeCacertStore(JdbcTestHelper.getInstance().getDataSource(), container);

	}

	@Test
	public void testSchemaIsWellRegistered() {
		assertNotNull(JdbcTestHelper.getInstance().getDbSchemaService().getSchemaDescriptor("smime-schema"));
	}

	@Test
	public void testStoreAndRetrieveWithUid() throws SQLException {
		String uid = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));
		Item item = itemStore.get(uid);
		assertNotNull(item);

		SmimeCacert certif = defaultSmimeCacert();
		smimeStore.create(item, certif);

		SmimeCacert td = smimeStore.get(item);

		assertNotNull(td);
		assertEquals(certif.cert, td.cert);

		item.id = new Random().nextInt();
		td = smimeStore.get(item);
		assertNull(td);
	}

	@Test
	public void testStoreRetrieveAndUpdate() throws SQLException {

		String uid = "test_" + System.nanoTime();
		itemStore.create(Item.create(uid, UUID.randomUUID().toString()));
		Item item = itemStore.get(uid);
		assertNotNull(item);

		SmimeCacert certif = defaultSmimeCacert();
		certif.cert = "cert content";
		smimeStore.create(item, certif);

		SmimeCacert td = smimeStore.get(item);
		assertNotNull(td);
		assertEquals(certif.cert, td.cert);

		td.cert = "updated cert content";

		smimeStore.update(item, td);
		SmimeCacert updated = smimeStore.get(item);
		assertNotNull(updated);

		assertNotNull(td);
		assertEquals(updated.cert, td.cert);
	}

	@Test
	public void testDelete() throws SQLException {
		String uid = "test_" + System.nanoTime();
		SmimeCacert cert = defaultSmimeCacert();

		createAndGet(uid, cert);
		Item item = itemStore.get(uid);
		assertNotNull(item);

		smimeStore.delete(item);
		assertNull(smimeStore.get(item));
	}

	@Test
	public void testDeleteAll() throws SQLException {
		String uid = "test_" + System.nanoTime();
		SmimeCacert todo = defaultSmimeCacert();
		createAndGet(uid, todo);
		Item item = itemStore.get(uid);
		assertNotNull(item);

		String uid2 = "test_" + System.nanoTime();
		SmimeCacert todo2 = defaultSmimeCacert();
		createAndGet(uid2, todo2);
		Item item2 = itemStore.get(uid2);
		assertNotNull(item2);

		smimeStore.deleteAll();
		assertNull(smimeStore.get(item));
		assertNull(smimeStore.get(item2));
	}

	private SmimeCacert createAndGet(String uid, SmimeCacert todo) {
		try {
			itemStore.create(Item.create(uid, UUID.randomUUID().toString()));
			Item item = itemStore.get(uid);
			smimeStore.create(item, todo);
			return smimeStore.get(item);
		} catch (SQLException e) {
			logger.error("error during S/MIME cacerts persistence call", e);
			fail(e.getMessage());
			return null;
		}

	}

}
