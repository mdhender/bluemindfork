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
package net.bluemind.backend.mail.replica.persistence.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.sql.SQLException;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.backend.cyrus.replication.testhelper.MailboxUniqueId;
import net.bluemind.backend.mail.replica.api.AppendTx;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.backend.mail.replica.api.utils.Subtree;
import net.bluemind.backend.mail.replica.persistence.MailboxReplicaStore;
import net.bluemind.backend.mail.replica.utils.SubtreeContainer;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.tests.BmTestContext;

public class MailboxReplicaStoreTests {

	private ItemStore itemStore;
	private MailboxReplicaStore boxReplicaStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		SecurityContext securityContext = SecurityContext.ANONYMOUS;
		BmTestContext bmContext = new BmTestContext(securityContext);

		ContainerStore containerHome = new ContainerStore(bmContext, JdbcTestHelper.getInstance().getDataSource(),
				securityContext);
		MailboxReplicaRootDescriptor rootDesc = MailboxReplicaRootDescriptor.create(Namespace.users,
				"user" + System.currentTimeMillis());
		String partition = "t" + System.currentTimeMillis() + "_vmw";
		Subtree sub = SubtreeContainer.mailSubtreeUid(partition, rootDesc.ns, rootDesc.name);
		String containerId = sub.subtreeUid();
		Container container = Container.create(containerId, IMailReplicaUids.REPLICATED_MBOXES, "test", "me", true);
		container = containerHome.create(container);

		assertNotNull(container);

		itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), container, securityContext);
		boxReplicaStore = new MailboxReplicaStore(JdbcTestHelper.getInstance().getDataSource(), container, partition);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCrudSimple() throws SQLException {
		MailboxReplica mb = simpleReplica();
		String uniqueId = MailboxUniqueId.random();
		itemStore.create(Item.create(uniqueId, null));
		Item it = itemStore.get(uniqueId);
		boxReplicaStore.create(it, mb);
		MailboxReplica reloaded = boxReplicaStore.get(it);
		assertNotNull(reloaded);

		reloaded.name = "updated";
		boxReplicaStore.update(it, reloaded);
		MailboxReplica reloaded2 = boxReplicaStore.get(it);
		assertEquals("updated", reloaded2.name);

		AppendTx append = boxReplicaStore.prepareAppend(it.id);
		assertNotNull(append);
		System.err.println("append: " + append);

		boxReplicaStore.delete(it);
		reloaded = boxReplicaStore.get(it);
		assertNull(reloaded);
	}

	@Test
	public void testCreateTwoDeleteAll() throws SQLException {
		MailboxReplica mb = simpleReplica();
		String uniqueId = MailboxUniqueId.random();
		itemStore.create(Item.create(uniqueId, null));
		Item it = itemStore.get(uniqueId);
		boxReplicaStore.create(it, mb);

		MailboxReplica mb2 = simpleReplica();
		mb2.name = "another";
		mb2.fullName = "another";
		String uniqueId2 = MailboxUniqueId.random();
		itemStore.create(Item.create(uniqueId2, null));
		Item it2 = itemStore.get(uniqueId2);
		boxReplicaStore.create(it2, mb2);

		MailboxReplica reloaded = boxReplicaStore.get(it);
		assertNotNull(reloaded);
		assertEquals(mb.name, reloaded.name);
		MailboxReplica reloaded2 = boxReplicaStore.get(it2);
		assertNotNull(reloaded2);
		assertEquals("another", reloaded2.name);

		boxReplicaStore.deleteAll();
		reloaded = boxReplicaStore.get(it);
		assertNull(reloaded);
		reloaded2 = boxReplicaStore.get(it2);
		assertNull(reloaded2);
	}

	@Test
	public void testAclsPersistence() throws SQLException {
		MailboxReplica mb = simpleReplica();
		mb.acls = Arrays.asList(MailboxReplica.Acl.create("admin0", "lrsp"));
		String uniqueId = MailboxUniqueId.random();
		itemStore.create(Item.create(uniqueId, null));
		Item it = itemStore.get(uniqueId);
		boxReplicaStore.create(it, mb);

		MailboxReplica reloaded = boxReplicaStore.get(it);
		assertNotNull(reloaded);
		assertNotNull(reloaded.acls);
		assertEquals("Reloaded acls count is incorrect", 1, reloaded.acls.size());
	}

	private MailboxReplica simpleReplica() {
		MailboxReplica mr = new MailboxReplica();
		mr.name = "INBOX";
		mr.fullName = mr.name;
		mr.uidValidity = System.currentTimeMillis();
		mr.options = "P";
		return mr;
	}

}
