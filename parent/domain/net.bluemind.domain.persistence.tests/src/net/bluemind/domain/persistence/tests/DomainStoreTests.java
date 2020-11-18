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
package net.bluemind.domain.persistence.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.config.InstallationId;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.persistence.DomainStore;

public class DomainStoreTests {

	private ItemStore domainItemStore;
	private DomainStore domainStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerStore = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				securityContext);

		String installationId = InstallationId.getIdentifier();
		Container domains = Container.create(installationId + "_domains", "domains", "domains container", "system",
				true);
		domains = containerStore.create(domains);
		assertNotNull(domains);

		domainItemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), domains, securityContext);

		domainStore = new DomainStore(JdbcTestHelper.getInstance().getDataSource());

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCreateAndGet() throws Exception {
		domainItemStore.create(Item.create("test", null));
		Item item = domainItemStore.get("test");

		Domain expected = new Domain();
		expected.name = "nameTest";
		expected.label = "label test";
		expected.global = true;
		expected.description = "ma main dans ta gueule";
		expected.aliases = new HashSet<>(Arrays.asList("test.fr", "toto.fr", "mel.gibson"));
		domainStore.create(item, expected);

		Domain actual = domainStore.get(item);
		assertNotNull(actual);
		assertEquals(expected.name, actual.name);
		assertEquals(expected.label, actual.label);
		assertEquals(expected.description, actual.description);
		assertEquals(expected.aliases, actual.aliases);
		assertEquals(expected.global, actual.global);
	}

	@Test
	public void testUpdateAndGet() throws Exception {
		domainItemStore.create(Item.create("test", null));
		Item item = domainItemStore.get("test");

		Domain expected = new Domain();
		expected.name = "nameTest";
		expected.label = "label test";
		expected.global = true;
		expected.description = "ma main dans ta gueule";
		expected.aliases = new HashSet<>(Arrays.asList("test.fr", "toto.fr", "mel.gibson"));

		domainStore.create(item, expected);

		expected.name = "unameTest";
		expected.label = "ulabel test";
		expected.global = false;
		expected.description = "ma main dans ta bouche";
		expected.aliases = new HashSet<>(Arrays.asList("jojo.fr"));

		domainStore.update(item, expected);

		Domain actual = domainStore.get(item);
		assertNotNull(actual);
		assertEquals(expected.name, actual.name);
		assertEquals(expected.label, actual.label);
		assertEquals(expected.description, actual.description);
		assertEquals(expected.aliases, actual.aliases);
		assertEquals(expected.global, actual.global);
	}

	@Test
	public void testDeleteAndGet() throws Exception {
		domainItemStore.create(Item.create("test", null));
		Item item = domainItemStore.get("test");

		Domain expected = new Domain();
		expected.name = "nameTest";
		expected.label = "label test";
		expected.global = true;
		expected.description = "ma main dans ta gueule";
		expected.aliases = new HashSet<>(Arrays.asList("test.fr", "toto.fr", "mel.gibson"));

		domainStore.create(item, expected);

		domainStore.delete(item);
		domainItemStore.delete(item);

		Domain actual = domainStore.get(item);
		assertNull(actual);
	}

	@Test
	public void testFindByNameOrAliases() throws Exception {
		domainItemStore.create(Item.create("test", null));
		Item item = domainItemStore.get("test");

		Domain expected = new Domain();
		expected.name = "name.fr";
		expected.label = "label test";
		expected.global = true;
		expected.description = "ma main dans ta gueule";
		expected.aliases = new HashSet<>(Arrays.asList("test.fr", "toto.fr", "mel.gibson"));

		domainStore.create(item, expected);

		assertEquals("test", domainStore.findByNameOrAliases("name.fr"));
		assertEquals("test", domainStore.findByNameOrAliases("test.fr"));
		assertNull(domainStore.findByNameOrAliases("fake.fr"));
	}

	@Test
	public void testCustomProperties() throws Exception {
		domainItemStore.create(Item.create("test", null));
		Item item = domainItemStore.get("test");

		Domain expected = new Domain();
		expected.name = "nameTest";
		expected.label = "label test";
		expected.global = true;
		expected.description = "ma main dans ta gueule";
		expected.aliases = new HashSet<>(Arrays.asList("test.fr", "toto.fr", "mel.gibson"));

		domainStore.create(item, expected);

		Domain actual = domainStore.get(item);
		assertEquals(0, actual.properties.size());

		Map<String, String> properties = new HashMap<String, String>();
		expected.properties = properties;
		domainStore.update(item, expected);
		actual = domainStore.get(item);
		assertEquals(0, actual.properties.size());

		properties.put("marco", "polo");
		expected.properties = properties;
		domainStore.update(item, expected);
		actual = domainStore.get(item);
		assertEquals(1, actual.properties.size());
		assertEquals("polo", actual.properties.get("marco"));
	}

}
