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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.SQLException;
import java.util.Arrays;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.config.Token;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.IFlatHierarchyUids;
import net.bluemind.core.container.api.internal.IInternalContainersFlatHierarchy;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ContainersHierarchyNodeStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class InternalContainersHierarchyServiceTests {
	private ContainerStore containerStore;
	private SecurityContext domainAdminSecurityContext;
	private SecurityContext user;
	private SecurityContext admin0SecurityContext;
	private String domainUid;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		PopulateHelper.initGlobalVirt();
		domainUid = "bmtest.lan";
		PopulateHelper.createTestDomain(domainUid);

		admin0SecurityContext = new SecurityContext(Token.admin0(), "admin0", Arrays.<String>asList(),
				Arrays.<String>asList(SecurityContext.ROLE_SYSTEM), "global.virt");
		user = new SecurityContext("testSessionId", "test", Arrays.<String>asList(), Arrays.<String>asList(),
				domainUid);

		domainAdminSecurityContext = new SecurityContext("testSessionId2", "admin", Arrays.<String>asList(),
				Arrays.<String>asList(SecurityContext.ROLE_ADMIN), domainUid);

		Sessions.get().put(admin0SecurityContext.getSessionId(), admin0SecurityContext);
		Sessions.get().put(user.getSessionId(), user);
		Sessions.get().put(domainAdminSecurityContext.getSessionId(), domainAdminSecurityContext);

		containerStore = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(), admin0SecurityContext);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testInit_CreatesTheExpectedContainer() throws SQLException {
		InternalContainersHierarchyMgmtService internalHierarchy = mgmtService();
		internalHierarchy.init();
		Container userHierContainer = containerStore
				.get(IFlatHierarchyUids.getIdentifier(user.getSubject(), user.getContainerUid()));
		assertNotNull(userHierContainer);
	}

	private InternalContainersHierarchyMgmtService mgmtService() {
		InternalContainersHierarchyMgmtService internalHierarchy = new InternalContainersHierarchyMgmtService(
				new BmTestContext(admin0SecurityContext), user.getSubject(), user.getContainerUid());
		return internalHierarchy;
	}

	@Test
	public void testInit_SecondInitDoesNothing() {
		InternalContainersHierarchyMgmtService internalHierarchy = mgmtService();
		internalHierarchy.init();
		try {
			internalHierarchy.init();
		} catch (Exception e) {
			fail();
		}
	}

	@Test
	public void tesCreate() throws SQLException {
		mgmtService().init();
		Container userHierContainer = containerStore
				.get(IFlatHierarchyUids.getIdentifier(user.getSubject(), user.getContainerUid()));
		ContainerHierarchyNode chn = new ContainerHierarchyNode();
		chn.containerUid = "daenerys";
		chn.containerType = "targaryen";
		chn.name = "Daenery Targaryen";
		BmTestContext ctx = new BmTestContext(admin0SecurityContext);

		DataSource ds = DataSourceRouter.get(ctx, userHierContainer.uid);

		ContainersHierarchyNodeStore itemValueStore = new ContainersHierarchyNodeStore(ds, userHierContainer);
		ContainerStoreService<ContainerHierarchyNode> storeService = new ContainerStoreService<>(ds,
				admin0SecurityContext, userHierContainer, itemValueStore, new ContainerHierarchyFlagProvider(),
				(v) -> 0L, seed -> seed);

		IInternalContainersFlatHierarchy internalHierarchy = new InternalContainersHierarchyService(ctx,
				ctx.getDataSource(), userHierContainer, new ContainersHierarchyEventProducer(user.getContainerUid(),
						user.getSubject(), VertxPlatform.eventBus()),
				storeService);
		internalHierarchy.create("mother_of_dragons", chn);
	}

	@Test
	public void testItemFlagDeleted() throws SQLException {
		mgmtService().init();
		Container userHierContainer = containerStore
				.get(IFlatHierarchyUids.getIdentifier(user.getSubject(), user.getContainerUid()));
		ContainerHierarchyNode chn = new ContainerHierarchyNode();
		chn.containerUid = "daenerys";
		chn.containerType = "targaryen";
		chn.name = "Daenery Targaryen";
		BmTestContext ctx = new BmTestContext(admin0SecurityContext);

		DataSource ds = DataSourceRouter.get(ctx, userHierContainer.uid);

		ContainersHierarchyNodeStore itemValueStore = new ContainersHierarchyNodeStore(ds, userHierContainer);
		ContainerStoreService<ContainerHierarchyNode> storeService = new ContainerStoreService<>(ds,
				admin0SecurityContext, userHierContainer, itemValueStore, new ContainerHierarchyFlagProvider(),
				(v) -> 0L, seed -> seed);

		IInternalContainersFlatHierarchy internalHierarchy = new InternalContainersHierarchyService(ctx,
				ctx.getDataSource(), userHierContainer, new ContainersHierarchyEventProducer(user.getContainerUid(),
						user.getSubject(), VertxPlatform.eventBus()),
				storeService);
		internalHierarchy.create("mother_of_dragons", chn);

		chn.deleted = true;
		internalHierarchy.update("mother_of_dragons", chn);

		ItemValue<ContainerHierarchyNode> mod = internalHierarchy.getComplete("mother_of_dragons");

		assertTrue(mod.flags.contains(ItemFlag.Deleted));
	}

}
