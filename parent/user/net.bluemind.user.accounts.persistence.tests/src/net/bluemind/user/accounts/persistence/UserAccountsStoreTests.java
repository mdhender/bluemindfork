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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.user.accounts.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.UserAccount;
import net.bluemind.user.api.UserAccountInfo;

public class UserAccountsStoreTests {

	private ItemStore itemStore;
	private UserAccountsStore store;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		
		PopulateHelper.initGlobalVirt();

		ContainerStore containerStore = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM);

		Container container = new Container();
		container.domainUid = "test.loc";
		container.name = "test";
		container.owner = "system";
		container.type = "dir";
		String uid = UUID.randomUUID().toString();
		container.uid = uid;
		container = containerStore.create(container);

		itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), container, SecurityContext.SYSTEM);
		store = new UserAccountsStore(JdbcTestHelper.getInstance().getDataSource());
	}

	@Test
	public void testCreatingAUserAccount() throws Exception {
		Item item = Item.create("user1", null);
		item = itemStore.create(item);

		UserAccount account = new UserAccount();
		account.login = "myLogin";
		account.credentials = "top-secret";
		String externalSystemId = "xCloud";
		account.additionalSettings = new HashMap<>();
		account.additionalSettings.put("key1", "value1");
		account.additionalSettings.put("key2", "value2");
		store.create(item, externalSystemId, account);

		UserAccount userAccount = store.get(item, externalSystemId);

		assertEquals(account.login, userAccount.login);
		assertEquals(account.credentials, userAccount.credentials);
		assertEquals(account.additionalSettings, userAccount.additionalSettings);
	}

	@Test
	public void testUpdatingAUserAccount() throws Exception {
		Item item = Item.create("user1", null);
		item = itemStore.create(item);

		UserAccount account = new UserAccount();
		account.login = "myLogin";
		account.credentials = "top-secret";
		String externalSystemId = "xCloud";
		account.additionalSettings = new HashMap<>();
		account.additionalSettings.put("key1", "value1");
		account.additionalSettings.put("key2", "value2");
		store.create(item, externalSystemId, account);

		account.login = "myLogin-updated";
		account.credentials = "top-secret-updated";
		account.additionalSettings = new HashMap<>();
		account.additionalSettings.put("key1-updated", "value1-updated");
		account.additionalSettings.put("key2-updated", "value2-updated");
		store.update(item, externalSystemId, account);

		UserAccount userAccount = store.get(item, externalSystemId);

		assertEquals(account.login, userAccount.login);
		assertEquals(account.credentials, userAccount.credentials);
		assertEquals(account.additionalSettings, userAccount.additionalSettings);
	}

	@Test
	public void testDeletingAUserAccount() throws Exception {
		Item item = Item.create("user1", null);
		item = itemStore.create(item);

		String externalSystemId1 = "xCloud1";
		String externalSystemId2 = "xCloud2";

		UserAccount account = new UserAccount();
		account.login = "myLogin";
		account.credentials = "top-secret";
		account.additionalSettings = new HashMap<>();
		account.additionalSettings.put("key1", "value1");
		account.additionalSettings.put("key2", "value2");

		assertNull(store.get(item, externalSystemId1));
		assertNull(store.get(item, externalSystemId2));

		store.create(item, externalSystemId1, account);
		store.create(item, externalSystemId2, account);

		assertNotNull(store.get(item, externalSystemId1));
		assertNotNull(store.get(item, externalSystemId2));

		store.delete(item, externalSystemId1);
		assertNull(store.get(item, externalSystemId1));
		assertNotNull(store.get(item, externalSystemId2));
	}

	@Test
	public void testDeletingAllUserAccounts() throws Exception {
		Item item = Item.create("user1", null);
		item = itemStore.create(item);

		String externalSystemId1 = "xCloud1";
		String externalSystemId2 = "xCloud2";

		UserAccount account = new UserAccount();
		account.login = "myLogin";
		account.credentials = "top-secret";
		account.additionalSettings = new HashMap<>();
		account.additionalSettings.put("key1", "value1");
		account.additionalSettings.put("key2", "value2");

		assertNull(store.get(item, "xCloud"));
		assertNull(store.get(item, "xCloud2"));

		store.create(item, externalSystemId1, account);
		store.create(item, externalSystemId2, account);

		Item someoneElse = Item.create("someone-else", null);
		someoneElse = itemStore.create(someoneElse);
		store.create(someoneElse, externalSystemId2, account);

		assertNotNull(store.get(item, externalSystemId1));
		assertNotNull(store.get(item, externalSystemId2));
		assertNotNull(store.get(someoneElse, externalSystemId2));

		store.deleteAll(item);
		assertNull(store.get(item, externalSystemId1));
		assertNull(store.get(item, externalSystemId2));
		assertNotNull(store.get(someoneElse, externalSystemId2));

	}

	@Test
	public void testGettingAllUserAccounts() throws Exception {
		Item item = Item.create("user1", null);
		item = itemStore.create(item);

		String externalSystemId1 = "xCloud1";
		String externalSystemId2 = "xCloud2";

		UserAccount account = new UserAccount();
		account.login = "myLogin";
		account.credentials = "top-secret";
		account.additionalSettings = new HashMap<>();
		account.additionalSettings.put("key1", "value1");
		account.additionalSettings.put("key2", "value2");

		assertNull(store.get(item, "xCloud"));
		assertNull(store.get(item, "xCloud2"));

		store.create(item, externalSystemId1, account);
		store.create(item, externalSystemId2, account);

		Item someoneElse = Item.create("someone-else", null);
		someoneElse = itemStore.create(someoneElse);
		store.create(someoneElse, externalSystemId1, account);

		assertNotNull(store.get(item, externalSystemId1));
		assertNotNull(store.get(item, externalSystemId2));
		assertNotNull(store.get(someoneElse, externalSystemId1));

		List<UserAccountInfo> me = store.getAll(item);
		List<UserAccountInfo> other = store.getAll(someoneElse);
		assertEquals(2, me.size());
		assertContains(me, externalSystemId1, externalSystemId2);
		assertEquals(1, other.size());
		assertContains(other, externalSystemId1);

	}

	private void assertContains(List<UserAccountInfo> infos, String... externalSystemIds) {
		int matches = 0;
		for (UserAccountInfo account : infos) {
			for (int i = 0; i < externalSystemIds.length; i++) {
				if (account.externalSystemId.equals(externalSystemIds[i])) {
					matches++;
				}
			}
		}
		assertEquals(matches, externalSystemIds.length);
	}

}
