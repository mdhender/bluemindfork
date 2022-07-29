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
package net.bluemind.documentfolder.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.AclStore;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.documentfolder.api.DocumentFolder;
import net.bluemind.documentfolder.api.IDocumentFolder;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class DocumentFolderServiceTests {
	private SecurityContext context;
	private Container container;
	private ContainerStore containerStore;
	private String domainUid;

	public IDocumentFolder getService() throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(IDocumentFolder.class, container.uid);
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		PopulateHelper.initGlobalVirt();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		domainUid = "domain" + System.currentTimeMillis() + ".lan";

		context = new SecurityContext("admin", "admin_" + domainUid, Arrays.<String>asList(), Arrays.<String>asList(),
				domainUid);

		Sessions.get().put(context.getSessionId(), context);

		PopulateHelper.createTestDomain(domainUid);
		PopulateHelper.addDomainAdmin("admin", domainUid);

		containerStore = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(), context);

		String containerId = "test" + System.nanoTime();
		container = Container.create(containerId, "test", "test", "me", domainUid, true);
		container = containerStore.create(container);
		container = containerStore.get(containerId);

		AclStore aclStore = new AclStore(JdbcTestHelper.getInstance().getDataSource());
		aclStore.store(container, Arrays.asList(AccessControlEntry.create(context.getSubject(), Verb.All)));
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testDomainHook() throws SQLException {
		Container dfContainer = containerStore.get("documentfolder_" + domainUid);
		assertNotNull(dfContainer);
	}

	@Test
	public void testDocumentFolder() throws ServerFault {
		IDocumentFolder service = getService();

		ListResult<DocumentFolder> folders = service.list();
		assertEquals(0, folders.total);

		String folder1Uid = UUID.randomUUID().toString();
		service.create(folder1Uid, "Folder 1");

		DocumentFolder df = service.get(folder1Uid);
		assertNotNull(df);
		assertEquals(folder1Uid, df.uid);
		assertEquals("Folder 1", df.name);

		service.rename(folder1Uid, "Updated folder 1");
		df = service.get(folder1Uid);
		assertNotNull(df);
		assertEquals(folder1Uid, df.uid);
		assertEquals("Updated folder 1", df.name);

		folders = service.list();
		assertEquals(1, folders.total);

		String folder2Uid = UUID.randomUUID().toString();
		service.create(folder2Uid, "Folder 2");

		folders = service.list();
		assertEquals(2, folders.total);

		service.delete(folder1Uid);

		folders = service.list();
		assertEquals(1, folders.total);
	}
}
