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
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.resource.api.type.ResourceType;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;
import net.bluemind.resource.api.type.ResourceTypeDescriptor.Property;
import net.bluemind.resource.persistence.ResourceTypeStore;

public class ResourceTypeStoreTests {

	private ResourceTypeStore resourceTypeStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		SecurityContext securityContext = SecurityContext.ANONYMOUS;

		ContainerStore containerStore = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
				securityContext);

		Container resources = Container.create("resource_test", "resources", "resources container", "system", true);
		resources = containerStore.create(resources);
		assertNotNull(resources);

		resourceTypeStore = new ResourceTypeStore(JdbcTestHelper.getInstance().getDataSource(), resources);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testCreateGetUpdateDelete() throws Exception {
		ResourceTypeDescriptor testDescriptor = ResourceTypeDescriptor.create("Event room", //
				ResourceTypeDescriptor.Property.create("WhiteBoard", ResourceTypeDescriptor.Property.Type.Boolean,
						"whiteboard ?"), //

				ResourceTypeDescriptor.Property.create("Seats", ResourceTypeDescriptor.Property.Type.Number,
						"number of seats"), //
				ResourceTypeDescriptor.Property.create("BlaBla", ResourceTypeDescriptor.Property.Type.String,
						"blabla"));
		testDescriptor.templates.put("fr",
				"Ce template utilise la propriété WhiteBoard de valeur ${WhiteBoard} et Seats de valeur ${Seats}");
		testDescriptor.templates.put("en",
				"This template uses the property WhiteBoard having the value ${WhiteBoard} and the property Seats having the value ${Seats}");
		resourceTypeStore.create("Room/EventRoom", testDescriptor);

		ResourceTypeDescriptor get = resourceTypeStore.get("Room/EventRoom");
		assertResourceTypeDescriptorEquals(get, testDescriptor);

		testDescriptor.label = "Vroom";
		testDescriptor.properties = Arrays.asList(
				ResourceTypeDescriptor.Property.create("TV", ResourceTypeDescriptor.Property.Type.Boolean, "TV ?"), //

				ResourceTypeDescriptor.Property.create("Bottles", ResourceTypeDescriptor.Property.Type.Number,
						"number of bottles"), //
				ResourceTypeDescriptor.Property.create("BlaBla", ResourceTypeDescriptor.Property.Type.String,
						"blabla"));
		resourceTypeStore.update("Room/EventRoom", testDescriptor);
		get = resourceTypeStore.get("Room/EventRoom");
		assertResourceTypeDescriptorEquals(get, testDescriptor);

		resourceTypeStore.delete("Room/EventRoom");
		get = resourceTypeStore.get("Room/EventRoom");
		assertNull(get);
	}

	@Test
	public void testList() throws Exception {
		resourceTypeStore.create("Room/EventRoom", ResourceTypeDescriptor.create("Event room"));
		resourceTypeStore.create("Vehicle/F1", ResourceTypeDescriptor.create("BM formula one"));

		List<ResourceType> res = resourceTypeStore.getTypes();
		assertEquals(2, res.size());
		assertEquals("Room/EventRoom", res.get(0).identifier);
		assertEquals("Event room", res.get(0).label);

		assertEquals("Vehicle/F1", res.get(1).identifier);
		assertEquals("BM formula one", res.get(1).label);
	}

	private void assertResourceTypeDescriptorEquals(ResourceTypeDescriptor get, ResourceTypeDescriptor testDescriptor) {
		assertEquals(get.label, testDescriptor.label);
		assertEquals(get.properties.size(), testDescriptor.properties.size());
		for (int i = 0; i < get.properties.size(); i++) {
			assertPropertyEquals(get.properties.get(i), testDescriptor.properties.get(i));
		}
		assertEquals(get.templates.size(), testDescriptor.templates.size());
		testDescriptor.templates.forEach((key, value) -> {
			assertTrue(get.templates.containsKey(key));
			assertEquals(get.templates.get(key), value);
		});
	}

	private void assertPropertyEquals(Property property, Property property2) {
		assertEquals(property.id, property2.id);
		assertEquals(property.label, property2.label);
		assertEquals(property.type, property2.type);
	}
}
