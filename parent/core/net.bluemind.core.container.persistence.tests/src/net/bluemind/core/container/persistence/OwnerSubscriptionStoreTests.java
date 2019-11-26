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
package net.bluemind.core.container.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.container.api.ContainerSubscriptionModel;
import net.bluemind.core.container.api.IOwnerSubscriptionUids;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.tests.BmTestContext;

public class OwnerSubscriptionStoreTests {

	private String fakeContainer;
	private OwnerSubscriptionStore hierStore;
	private ItemStore nodeItemStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		SecurityContext securityContext = SecurityContext.ANONYMOUS;
		BmTestContext ctx = new BmTestContext(securityContext);

		ContainerStore containerStore = new ContainerStore(ctx, JdbcTestHelper.getInstance().getDataSource(),
				securityContext);
		fakeContainer = IOwnerSubscriptionUids.getIdentifier("me", "fake_domain");
		Container fais = Container.create(fakeContainer, IOwnerSubscriptionUids.TYPE, fakeContainer, "me", true);
		fais = containerStore.create(fais);

		hierStore = new OwnerSubscriptionStore(JdbcTestHelper.getInstance().getDataSource(), fais);
		nodeItemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), fais, securityContext);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void createThenGetNode() throws Exception {
		String uid = "" + System.currentTimeMillis();
		Item item = nodeItemStore.create(Item.create(uid, null));
		ContainerSubscriptionModel node = ContainerSubscriptionModel.create("cont_uid", "cont_type", "another", true,
				false, "Yeah Cont");
		hierStore.create(item, node);
		ContainerSubscriptionModel got = hierStore.get(item);
		assertEquals(node.containerType, got.containerType);
		assertEquals(node.containerUid, got.containerUid);
		assertEquals(node.offlineSync, got.offlineSync);
		assertEquals(node.owner, got.owner);
		assertEquals(node.defaultContainer, got.defaultContainer);
		assertEquals(node.name, got.name);
	}

	@Test
	public void createThenDeleteNode() throws Exception {
		String uid = "" + System.currentTimeMillis();
		Item item = nodeItemStore.create(Item.create(uid, null));
		ContainerSubscriptionModel node = ContainerSubscriptionModel.create("cont_uid", "cont_type", "another", true,
				false, "Yeah Cont");
		hierStore.create(item, node);
		ContainerSubscriptionModel got = hierStore.get(item);
		assertNotNull(got);
		hierStore.delete(item);
		got = hierStore.get(item);
		assertNull(got);
	}

	@Test
	public void updateNode() throws Exception {
		String uid = "" + System.currentTimeMillis();
		Item item = nodeItemStore.create(Item.create(uid, null));
		ContainerSubscriptionModel node = ContainerSubscriptionModel.create("cont_uid", "cont_type", "another", true,
				false, "Yeah Cont");
		hierStore.create(item, node);

		node.containerUid = "cont_uid2";
		node.containerType = "cont_type2";
		node.owner = "third_party";
		node.offlineSync = true;
		node.defaultContainer = false;
		node.name = "Upd Yeah";
		hierStore.update(item, node);
		ContainerSubscriptionModel got = hierStore.get(item);
		assertEquals("cont_uid2", got.containerUid);
		assertEquals("cont_type2", got.containerType);
		assertEquals(true, got.offlineSync);
		assertEquals("third_party", got.owner);
		assertEquals(false, got.defaultContainer);
		assertEquals("Upd Yeah", got.name);

	}

}
