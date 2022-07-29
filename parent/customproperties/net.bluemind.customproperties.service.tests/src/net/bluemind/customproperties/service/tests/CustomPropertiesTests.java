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
package net.bluemind.customproperties.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.customproperties.api.CustomPropertiesRequirements;
import net.bluemind.customproperties.api.ICustomProperties;
import net.bluemind.lib.vertx.VertxPlatform;

public class CustomPropertiesTests {

	@Before
	public void before() throws Exception {
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
	}

	@Test
	public void testGetCustomProps() throws ServerFault {
		ICustomProperties service = getService();
		CustomPropertiesRequirements customProperties = service.get("vevent");

		assertNotNull(customProperties);
		assertEquals(2, customProperties.customProperties.size());
		assertEquals("junit-requester-id", customProperties.requesterId);

		customProperties = service.get("osef");
		assertNull(customProperties);
	}

	protected ICustomProperties getService() throws ServerFault {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ICustomProperties.class);
	}
}
