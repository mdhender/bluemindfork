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
package net.bluemind.authentication.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.authentication.api.APIKey;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class APIKeysTests {

	private APIKeysService service;

	@Before
	public void setup() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList(TagDescriptor.bm_es.getTag());

		PopulateHelper.initGlobalVirt(esServer);

		PopulateHelper.addDomainAdmin("admin0", "global.virt");

		SecurityContext ctx = new SecurityContext("testUser", "test", Arrays.<String>asList(), Arrays.<String>asList(),
				"fakeContainerUid");

		service = new APIKeysService(JdbcTestHelper.getInstance().getDataSource(), ctx);

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void securityContext() {
		service = new APIKeysService(JdbcTestHelper.getInstance().getDataSource(), SecurityContext.ANONYMOUS);
		try {
			service.create("anon sc");
			fail();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void emptyDisplayName() {
		try {
			service.create(null);
			fail();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			service.create("");
			fail();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void create() throws ServerFault {
		String dn = "key" + System.currentTimeMillis();
		APIKey ret = service.create(dn);
		assertNotNull(ret);
		assertEquals(dn, ret.displayName);
		assertNotNull(ret.sid);
		assertNotNull(ret.domainUid);
	}

	@Test
	public void createWithKey() throws ServerFault {
		String dn = "key" + System.currentTimeMillis();
		String key = UUID.randomUUID().toString();
		APIKey ret = service.store(key, dn);
		assertNotNull(ret);
		assertEquals(dn, ret.displayName);
		assertEquals(key, ret.sid);
		assertNotNull(ret.domainUid);
	}

	@Test
	public void get() throws ServerFault {
		String dn = "key" + System.currentTimeMillis();
		APIKey ret = service.create(dn);
		APIKey get = service.get(ret.sid);

		assertNotNull(get);
		assertEquals(ret.subject, get.subject);
		assertEquals(ret.sid, get.sid);
		assertEquals(ret.domainUid, get.domainUid);
		assertEquals(ret.displayName, get.displayName);
	}

	@Test
	public void fetch() throws ServerFault {

		List<APIKey> list = service.list();
		assertEquals(0, list.size());

		String dn1 = "key" + System.currentTimeMillis();
		APIKey ak1 = service.create(dn1);

		String dn2 = "key" + System.currentTimeMillis();
		APIKey ak2 = service.create(dn2);

		list = service.list();
		assertEquals(2, list.size());

		boolean ak1found = false;
		boolean ak2found = false;
		for (APIKey k : list) {
			if (k.sid.equals(ak1.sid)) {
				ak1found = true;
				assertEquals(ak1.displayName, k.displayName);
			}

			if (k.sid.equals(ak2.sid)) {
				ak2found = true;
				assertEquals(ak2.displayName, k.displayName);
			}
		}

		assertTrue(ak1found);
		assertTrue(ak2found);

	}

	@Test
	public void delete() throws ServerFault {
		APIKey key = service.create("key1");
		APIKey key2 = service.create("key2");

		List<APIKey> list = service.list();
		assertEquals(2, list.size());

		service.delete(key.sid);

		list = service.list();
		assertEquals(1, list.size());

		service.delete(key2.sid);

		list = service.list();
		assertEquals(0, list.size());

	}

}
