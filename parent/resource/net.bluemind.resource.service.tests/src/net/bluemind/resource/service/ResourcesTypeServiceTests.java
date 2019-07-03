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
package net.bluemind.resource.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.io.ByteStreams;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.resource.api.type.IResourceTypes;
import net.bluemind.resource.api.type.ResourceTypeDescriptor;
import net.bluemind.resource.persistance.ResourceTypeStore;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class ResourcesTypeServiceTests {
	protected String testDomainUid;
	private ResourceTypeStore store;
	private SecurityContext domainAdminSC;
	private SecurityContext badDomainAdminSC;
	private SecurityContext userSC;
	private static byte[] image;

	@BeforeClass
	public static void init() throws IOException {
		image = ByteStreams
				.toByteArray(ResourcesServiceTests.class.getClassLoader().getResourceAsStream("download.png"));
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		

		testDomainUid = "test.lan";
		PopulateHelper.initGlobalVirt();
		PopulateHelper.createTestDomain(testDomainUid);

		ContainerStore containerHome = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM);

		
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		domainAdminSC = BmTestContext.contextWithSession("d1", "admin", testDomainUid, SecurityContext.ROLE_ADMIN)
				.getSecurityContext();

		userSC = BmTestContext.contextWithSession("u1", "u1", testDomainUid).getSecurityContext();

		badDomainAdminSC = BmTestContext.contextWithSession("d2", "admin2", "fakeDomain", SecurityContext.ROLE_ADMIN)
				.getSecurityContext();

		String containerId = testDomainUid;
		Container container = containerHome.get(containerId);

		store = new ResourceTypeStore(JdbcActivator.getInstance().getDataSource(), container);
		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});
		launched.await();
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected IResourceTypes service(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(IResourceTypes.class, testDomainUid);

	}

	@Test
	public void testGet() throws Exception {
		String rtId = "Room/Reu";
		store.create(rtId, ResourceTypeDescriptor.create("test"));
		ResourceTypeDescriptor ret = service(userSC).get(rtId);
		assertNotNull(ret);

		assertNull(service(userSC).get("fakeId"));
	}

	@Test
	public void testUpdate() throws Exception {
		String rtId = "Room/Reu";
		store.create(rtId, ResourceTypeDescriptor.create("test"));
		service(domainAdminSC).update(rtId, ResourceTypeDescriptor.create("test2"));

		try {
			service(domainAdminSC).update("fakeId",

					ResourceTypeDescriptor.create("test2"));
			fail();
		} catch (ServerFault e) {
			// normal
		}

		try {
			service(badDomainAdminSC).update(rtId, ResourceTypeDescriptor.create("test2"));
			fail();
		} catch (ServerFault e) {
			// normal
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void testDelete() throws Exception {
		String rtId = "Room/Reu";
		store.create(rtId, ResourceTypeDescriptor.create("test"));
		service(domainAdminSC).delete(rtId);
		assertNull(store.get(rtId));
		try {
			service(domainAdminSC).delete("fakeId");
			fail();
		} catch (ServerFault e) {
			// normal
		}

		store.create(rtId, ResourceTypeDescriptor.create("test"));

		try {
			service(badDomainAdminSC).delete(rtId);

			fail();
		} catch (ServerFault e) {
			// normal
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

	}

	@Test
	public void testCreate() throws Exception {
		String rtId = "Room/Reu";
		service(domainAdminSC).create(rtId, ResourceTypeDescriptor.create("test"));
		assertNotNull(store.get(rtId));

		try {
			service(badDomainAdminSC).create(rtId + "2", ResourceTypeDescriptor.create("test"));

			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}
	}

	@Test
	public void testSetIcon() throws Exception {
		String rtId = "Room/Reu" + System.currentTimeMillis();
		service(domainAdminSC).create(rtId, ResourceTypeDescriptor.create("test"));
		assertNotNull(store.get(rtId));

		service(domainAdminSC).setIcon(rtId, image);

		// test bad image
		try {
			service(domainAdminSC).setIcon(rtId, "toto".getBytes());
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.INVALID_PARAMETER, e.getCode());
		}

		// test only admin can set icon
		try {
			service(badDomainAdminSC).setIcon(rtId, image);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.PERMISSION_DENIED, e.getCode());
		}

		// test set icon to inexistent resource type
		try {
			service(domainAdminSC).setIcon("fakeId", image);
			fail();
		} catch (ServerFault e) {
			assertEquals(ErrorCode.NOT_FOUND, e.getCode());
		}

	}

	@Test
	public void testGetIcon() throws Exception {
		String rtId = "Room/Reu" + System.currentTimeMillis();
		service(domainAdminSC).create(rtId, ResourceTypeDescriptor.create("test"));
		assertNotNull(store.get(rtId));

		// default icon
		assertNotNull(service(domainAdminSC).getIcon(rtId));
		service(domainAdminSC).setIcon(rtId, image);

		assertNotNull(service(domainAdminSC).getIcon(rtId));
		assertTrue(Arrays.equals(image, service(domainAdminSC).getIcon(rtId)));

		// test everyone can read icon
		try {
			service(badDomainAdminSC).getIcon(rtId);

		} catch (ServerFault e) {
			fail("should not fail");
		}

		// test set icon to inexistent resource type
		assertNull(service(domainAdminSC).getIcon("fakeId"));
	}

}
