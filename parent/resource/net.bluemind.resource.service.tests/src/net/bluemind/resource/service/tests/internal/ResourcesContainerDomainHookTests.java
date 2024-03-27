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
package net.bluemind.resource.service.tests.internal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.resource.api.type.IResourceTypeUids;
import net.bluemind.resource.service.internal.ResourcesContainerDomainHook;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class ResourcesContainerDomainHookTests {

	private ResourcesContainerDomainHook hook = new ResourcesContainerDomainHook();
	private BmTestContext testContext;
	private ItemValue<Domain> testDomain;
	private ContainerStore containerStore;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList(TagDescriptor.bm_es.getTag());

		PopulateHelper.initGlobalVirt(esServer);

		containerStore = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(), SecurityContext.SYSTEM);

		testContext = new BmTestContext(SecurityContext.SYSTEM);

		testDomain = new ItemValue<>();
		testDomain.uid = "test.lan";
		testDomain.value = Domain.create("test", "test", "test", new HashSet<String>());

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testOnCreated() throws Exception {
		PopulateHelper.createTestDomain("test.lan");
		// created has been called
		// hook.onCreated(testContext, testDomain);

		Container container = containerStore.get(IResourceTypeUids.getIdentifier(testDomain));
		assertNotNull(container);
	}

	@Test
	public void testOnUpdated() throws Exception {
		PopulateHelper.createTestDomain("test.lan");

		try {
			hook.onUpdated(testContext, testDomain, testDomain);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

}
