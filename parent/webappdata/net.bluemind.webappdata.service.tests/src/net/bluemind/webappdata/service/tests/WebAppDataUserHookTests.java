/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.webappdata.service.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.core.utils.UIDGenerator;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.webappdata.api.IWebAppData;
import net.bluemind.webappdata.api.IWebAppDataUids;
import net.bluemind.webappdata.api.WebAppData;

public class WebAppDataUserHookTests {

	private String domainUid;

	@Before
	public void setup() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(event -> launched.countDown());
		launched.await();

		PopulateHelper.initGlobalVirt();

		domainUid = "dom" + System.currentTimeMillis() + ".test";
		domainUid = PopulateHelper.createTestDomain(domainUid).uid;
	}

	@After
	public void teardown() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testUserWebAppDataLifecycle() throws Exception {
		String user = "user1";
		String userUid = PopulateHelper.addUser(user, domainUid);
		String webAppDataContainerUid = IWebAppDataUids.containerUid(user);

		IContainers containerService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainers.class);
		assertNotNull(containerService.get(webAppDataContainerUid));

		IWebAppData webAppDataService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IWebAppData.class, webAppDataContainerUid);
		String itemUid = UIDGenerator.uid();
		webAppDataService.create(itemUid, exampleWebAppData());
		assertNotNull(webAppDataService.get(itemUid));

		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
				domainUid);
		TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), userService.delete(userUid));

		assertNull(webAppDataService.get(itemUid));
		assertNull(containerService.getIfPresent(webAppDataContainerUid));
	}

	static WebAppData exampleWebAppData() {
		WebAppData data = new WebAppData();
		data.key = "mail-app:folders:expanded";
		data.value = "['1fa7ea8a59bbec5c', '3db4e71c5b519fad']";
		return data;
	}

}
