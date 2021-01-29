/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.videoconferencing.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.resource.api.type.IResourceTypes;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;
import net.bluemind.resource.api.type.ResourceTypeDescriptor.Property;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class ResourceTypeSanitizerTests extends AbstractVideoConferencingTests {

	@Test
	public void testResourceTypeSanitizer() throws Exception {
		PopulateHelper.addDomain(domainUid);

		IResourceTypes service = ServerSideServiceProvider.getProvider(context).instance(IResourceTypes.class,
				domainUid);
		ResourceTypeDescriptor resType = service.get("this-is-video-conferencing");
		assertNotNull(resType);

		// remove "Type" properties
		resType.properties = new ArrayList<>();
		service.update("this-is-video-conferencing", resType);

		resType = service.get("this-is-video-conferencing");
		List<Property> properties = resType.properties;
		assertEquals(1, properties.size());

		Property prop = properties.get(0);

		assertEquals("this-is-video-conferencing-type", prop.id);
		assertEquals("Type", prop.label);
		assertEquals(ResourceTypeDescriptor.Property.Type.String, prop.type);

	}
}
