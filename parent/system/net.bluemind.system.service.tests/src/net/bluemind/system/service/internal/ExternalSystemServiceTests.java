/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.system.service.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.system.api.ExternalSystem;
import net.bluemind.system.api.ExternalSystem.AuthKind;
import net.bluemind.system.api.IExternalSystem;

public class ExternalSystemServiceTests {

	IExternalSystem service;

	@Before
	public void setup() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IExternalSystem.class);
	}

	@Test
	public void testGettingExternalSystems_Priority() {
		List<ExternalSystem> externalSystems = service.getExternalSystems();

		assertEquals(2, externalSystems.size());
		assertEquals("TestSystem2", externalSystems.get(0).identifier);
		assertEquals("TestSystem1", externalSystems.get(1).identifier);
	}

	@Test
	public void testGettingExternalSystemShouldResolveAllValues() {
		ExternalSystem externalSystem = service.getExternalSystem("TestSystem1");

		assertEquals("TestSystem1", externalSystem.identifier);
		assertEquals("System 1", externalSystem.description);
		assertEquals(AuthKind.SIMPLE_CREDENTIALS, externalSystem.authKind);
		assertNull(service.getLogo(externalSystem.identifier));

		ExternalSystem externalSystem2 = service.getExternalSystem("TestSystem2");

		assertEquals("TestSystem2", externalSystem2.identifier);
		assertEquals("System 2", externalSystem2.description);
		assertEquals(AuthKind.API_KEY, externalSystem2.authKind);
		assertNotNull(service.getLogo(externalSystem2.identifier));

	}

}
