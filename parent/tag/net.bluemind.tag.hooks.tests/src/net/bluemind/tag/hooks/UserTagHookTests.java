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
package net.bluemind.tag.hooks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.tag.api.Tag;
import net.bluemind.tag.persistence.TagStore;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.User;
import net.bluemind.user.service.internal.ContainerUserStoreService;

public class UserTagHookTests {

	private ContainerStore containerStore;
	private AclStore aclStore;
	private UserTagHook userTagHook;

	private BmTestContext bmContext;

	private String domainUid = "fakedomain.lan";
	private String datalocation;
	private DataSource dataDataSource;
	private ContainerUserStoreService userStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		datalocation = PopulateHelper.FAKE_CYRUS_IP;
		dataDataSource = JdbcActivator.getInstance().getMailboxDataSource(datalocation);

		bmContext = new BmTestContext(SecurityContext.SYSTEM);
		containerStore = new ContainerStore(bmContext, dataDataSource, SecurityContext.SYSTEM);

		aclStore = new AclStore(bmContext, dataDataSource);
		userTagHook = new UserTagHook();

		PopulateHelper.initGlobalVirt();
		ItemValue<Domain> domain = PopulateHelper.createTestDomain(domainUid);

		Container domainContainer = new ContainerStore(bmContext, bmContext.getDataSource(), SecurityContext.SYSTEM)
				.get(domainUid);
		userStore = new ContainerUserStoreService(bmContext, domainContainer, domain);
	}

	@Test
	public void testOnCreated() throws SQLException {

		ItemValue<User> userItem = createTestUser();
		userTagHook.onUserCreated(bmContext, domainUid, userItem);

		Container tagContainer = containerStore.get("tags_" + userItem.uid);
		assertNotNull(tagContainer);

		List<AccessControlEntry> acl = aclStore.get(tagContainer);
		assertEquals(1, acl.size());
		AccessControlEntry ace = acl.get(0);
		assertEquals(Verb.All, ace.verb);
		assertEquals(userItem.uid, ace.subject);
	}

	@Test
	public void testOnDelete() throws SQLException {
		ItemValue<User> userItem = createTestUser();
		userTagHook.onUserCreated(bmContext, domainUid, userItem);
		Container tagContainer = containerStore.get("tags_" + userItem.uid);
		assertNotNull(tagContainer);

		// create tag
		ItemStore itemStore = new ItemStore(dataDataSource, tagContainer, SecurityContext.SYSTEM);
		itemStore.create(Item.create("tag", null));
		Item item = itemStore.get("tag");
		Tag tag = new Tag();
		tag.label = "test";
		tag.color = "00ff00";
		TagStore tagStore = new TagStore(dataDataSource, tagContainer);
		tagStore.create(item, tag);

		userTagHook.onUserDeleted(bmContext, domainUid, userItem);
		tagContainer = containerStore.get("tags_" + userItem.uid);
		assertNull(tagContainer);
		assertNull(tagStore.get(item));
	}

	private ItemValue<User> createTestUser() {
		User user = new User();
		user.login = "testUser_" + System.nanoTime();
		user.dataLocation = datalocation;
		String uid = UUID.randomUUID().toString();
		Item item = Item.create(uid, null);
		item.displayName = "test";
		ItemValue<User> ret = ItemValue.<User>create(item, user);
		// create dir entry with datalocation
		userStore.create(ret.uid, ret.value);
		return ret;
	}
}
