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
package net.bluemind.resource.persistence.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.Email;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.resource.api.ResourceDescriptor.PropertyValue;
import net.bluemind.resource.api.ResourceReservationMode;
import net.bluemind.resource.persistence.ResourceStore;

public class ResourceStoreTests {

	private ItemStore resourceItemStore;
	private ResourceStore resourceStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		
		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerStore = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				securityContext);

		Container resources = Container.create("testContainer", "resources", "test resources container", "system",
				true);
		resources = containerStore.create(resources);
		assertNotNull(resources);

		resourceItemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), resources, securityContext);

		resourceStore = new ResourceStore(JdbcTestHelper.getInstance().getDataSource(), resources);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCreateAndGet() throws Exception {
		resourceItemStore.create(Item.create("test", null));
		Item item = resourceItemStore.get("test");

		ResourceDescriptor expected = new ResourceDescriptor();
		expected.typeIdentifier = "testId";
		expected.label = "label test";
		expected.description = "ma main dans ta gueule";
		expected.reservationMode = ResourceReservationMode.OWNER_MANAGED;
		expected.dataLocation = "serverUid";
		expected.properties = Arrays.asList(ResourceDescriptor.PropertyValue.create("test1", "value1"),
				ResourceDescriptor.PropertyValue.create("test2", "value2"));

		resourceStore.create(item, expected);

		ResourceDescriptor actual = resourceStore.get(item);
		assertNotNull(actual);
		assertEquals(expected.label, actual.label);
		assertEquals(expected.description, actual.description);
		assertEquals(expected.reservationMode, actual.reservationMode);
		assertEquals(expected.dataLocation, actual.dataLocation);
		assertEquals(expected.properties.size(), actual.properties.size());
		for (int i = 0; i < actual.properties.size(); i++) {
			ResourceDescriptor.PropertyValue pac = actual.properties.get(i);

			PropertyValue pex = null;
			for (PropertyValue p : expected.properties) {
				if (p.propertyId.equals(pac.propertyId)) {
					pex = p;
				}
			}
			assertNotNull(pex);
			assertEquals(pex.value, pac.value);
		}
	}

	@Test
	public void testUpdateAndGet() throws Exception {
		resourceItemStore.create(Item.create("test", null));
		Item item = resourceItemStore.get("test");

		ResourceDescriptor expected = new ResourceDescriptor();
		expected.typeIdentifier = "testId";
		expected.label = "label test";
		expected.description = "ma main dans ta gueule";
		expected.reservationMode = ResourceReservationMode.AUTO_ACCEPT;
		expected.dataLocation = "serverUid";
		expected.properties = Arrays.asList(ResourceDescriptor.PropertyValue.create("test1", "value1"),
				ResourceDescriptor.PropertyValue.create("test2", "value2"));

		resourceStore.create(item, expected);

		expected.label = "ulabel test";
		expected.description = "ma main dans ta bouche";
		expected.reservationMode = ResourceReservationMode.OWNER_MANAGED;
		expected.dataLocation = "serverUid2";
		expected.properties = Arrays.asList(ResourceDescriptor.PropertyValue.create("test1", "value updated"));

		resourceStore.update(item, expected);
		ResourceDescriptor actual = resourceStore.get(item);
		assertNotNull(actual);
		assertEquals(expected.label, actual.label);
		assertEquals(expected.reservationMode, actual.reservationMode);
		assertEquals(expected.description, actual.description);
		assertEquals(expected.properties.size(), actual.properties.size());

		for (int i = 0; i < actual.properties.size(); i++) {
			ResourceDescriptor.PropertyValue pex = expected.properties.get(i);
			ResourceDescriptor.PropertyValue pac = actual.properties.get(i);
			assertEquals(pex.propertyId, pac.propertyId);
			assertEquals(pex.value, pac.value);
		}
	}

	@Test
	public void testDeleteAndGet() throws Exception {
		resourceItemStore.create(Item.create("test", null));
		Item item = resourceItemStore.get("test");

		ResourceDescriptor expected = new ResourceDescriptor();
		expected.typeIdentifier = "testId";
		expected.label = "label test";
		expected.description = "ma main dans ta gueule";
		expected.dataLocation = "serverUid";
		expected.reservationMode = ResourceReservationMode.AUTO_ACCEPT;
		expected.properties = Arrays.asList(ResourceDescriptor.PropertyValue.create("test1", "value1"),
				ResourceDescriptor.PropertyValue.create("test2", "value2"));

		resourceStore.create(item, expected);

		resourceStore.delete(item);
		resourceItemStore.delete(item);

		ResourceDescriptor actual = resourceStore.get(item);
		assertNull(actual);
	}

	@Test
	public void testCreateWithoutReservationMode_ShouldSetReservationModeTo_AUTO_ACCEPT_REFUSE() throws Exception {
		resourceItemStore.create(Item.create("test", null));
		Item item = resourceItemStore.get("test");

		ResourceDescriptor res = new ResourceDescriptor();
		res.typeIdentifier = "testId";
		res.label = "label test";
		res.description = "ma main dans ta gueule";
		res.dataLocation = "serverUid";
		res.properties = Arrays.asList(ResourceDescriptor.PropertyValue.create("test1", "value1"),
				ResourceDescriptor.PropertyValue.create("test2", "value2"));
		Email email1 = Email.create("punisher@fresse.de", true);
		res.emails = Arrays.asList(email1);

		resourceStore.create(item, res);

		ResourceDescriptor actual = resourceStore.get(item);
		assertEquals(ResourceReservationMode.AUTO_ACCEPT_REFUSE, actual.reservationMode);
	}

	@Test
	public void testFindByTypeId() throws Exception {

		ResourceDescriptor res = new ResourceDescriptor();
		res.typeIdentifier = "testId";
		res.label = "label test";
		res.dataLocation = "serverUid";

		resourceItemStore.create(Item.create("test", null));
		Item item = resourceItemStore.get("test");
		resourceStore.create(item, res);

		assertEquals(1, resourceStore.findByType(res.typeIdentifier).size());
		assertEquals(0, resourceStore.findByType("fakeId").size());

	}
}
