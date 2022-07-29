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

import java.util.Arrays;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.util.concurrent.SettableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.IContainersFlatHierarchy;
import net.bluemind.core.container.api.IFlatHierarchyUids;
import net.bluemind.core.container.api.internal.IInternalContainersFlatHierarchyMgmt;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ContainersHierarchyNodeStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class ContainersHierarchyServiceTests {

	private ContainerStore containerStore;
	private SecurityContext domainAdminSecurityContext;
	private SecurityContext user;
	private SecurityContext admin0SecurityContext;
	private String domainUid;
	private Container userHierContainer;

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

		containerStore = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(), admin0SecurityContext);

		IInternalContainersFlatHierarchyMgmt internalHierarchy = new InternalContainersHierarchyMgmtService(
				new BmTestContext(admin0SecurityContext), user.getSubject(), user.getContainerUid());
		internalHierarchy.init();
		userHierContainer = containerStore
				.get(IFlatHierarchyUids.getIdentifier(user.getSubject(), user.getContainerUid()));

		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected IContainersFlatHierarchy getService(SecurityContext securityContext) throws ServerFault {
		BmTestContext ctx = new BmTestContext(securityContext);

		DataSource ds = DataSourceRouter.get(ctx, userHierContainer.uid);

		ContainersHierarchyNodeStore itemValueStore = new ContainersHierarchyNodeStore(ds, userHierContainer);
		ContainerStoreService<ContainerHierarchyNode> storeService = new ContainerStoreService<>(ds, securityContext,
				userHierContainer, itemValueStore, new ContainerHierarchyFlagProvider(), (v) -> 0L, seed -> seed);

		return new InternalContainersHierarchyService(ctx, ctx.getDataSource(), userHierContainer,
				new ContainersHierarchyEventProducer(domainUid, user.getSubject(), VertxPlatform.eventBus()),
				storeService);
	}

	@Test
	public void testGetService() {
		IContainersFlatHierarchy service = getService(user);
		assertNotNull(service);
	}

	@Test
	public void testGetServiceWithServerSideProviderAsUser() {
		IContainersFlatHierarchy service = ServerSideServiceProvider.getProvider(new BmTestContext(user))
				.instance(IContainersFlatHierarchy.class, domainUid, user.getSubject());
		assertNotNull(service);
	}

	@Test
	public void testGetServiceWithServerSideProviderAsAdmin0() {
		IContainersFlatHierarchy service = ServerSideServiceProvider
				.getProvider(new BmTestContext(admin0SecurityContext))
				.instance(IContainersFlatHierarchy.class, domainUid, user.getSubject());
		assertNotNull(service);
	}

}
