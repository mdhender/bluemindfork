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
package net.bluemind.core.container.service.internal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;

public class ContainerManagementFactoryTests {

	private ContainerStore containerStore;
	private SecurityContext securityContext;
	private String containerId;
	private Container container;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		

		securityContext = new SecurityContext("testSessionId", "test", Arrays.<String> asList(),
				Arrays.<String> asList(), "fakeDomainUid");

		Sessions.get().put(securityContext.getSessionId(), securityContext);

		containerStore = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(), securityContext);

		containerId = "test_" + System.nanoTime();
		container = Container.create(containerId, "test", "test", "me", true);
		container = containerStore.create(container);
		assertNotNull(container);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testGet_containerNotFound() {

		try {
			ServerSideServiceProvider.getProvider(securityContext).instance(IContainerManagement.class, "fakeId");
			fail("should fail");
		} catch (ServerFault e) {
		}
	}

	@Test
	public void testGet() {
		IContainerManagement mgnt = null;

		try {
			mgnt = ServerSideServiceProvider.getProvider(securityContext).instance(IContainerManagement.class,
					containerId);
		} catch (ServerFault e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		assertNotNull(mgnt);
	}

}
