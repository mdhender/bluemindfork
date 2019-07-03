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
package net.bluemind.im.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SettableFuture;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.im.api.IInstantMessaging;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public abstract class AbstractIMServiceTests {

	private SecurityContext sysContext;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());
		ElasticsearchTestHelper.getInstance().beforeTest();
		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");
		PopulateHelper.initGlobalVirt(esServer);

		sysContext = BmTestContext.contextWithSession("sid3" + System.currentTimeMillis(), "admin0@global.virt",
				"global.virt", SecurityContext.ROLE_SYSTEM).getSecurityContext();

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

	protected abstract IInstantMessaging getService(SecurityContext context) throws ServerFault;

	@Test
	public void testRoster() throws ServerFault {
		String jabberId = "jabberId";
		String data = "data";

		String roster = getService(sysContext).getRoster(jabberId);

		assertNull(roster);

		getService(sysContext).setRoster(jabberId, data);

		roster = getService(sysContext).getRoster(jabberId);
		assertEquals(data, roster);

		data = "updated roster";
		getService(sysContext).setRoster(jabberId, data);

		roster = getService(sysContext).getRoster(jabberId);
		assertEquals(data, roster);
	}

	@Test
	public void testGetLastMessagesBetween() throws ServerFault {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("id", UUID.randomUUID().toString());
		map.put("timecreate", Calendar.getInstance().getTimeInMillis());
		map.put("from", "test@bm.lan");
		map.put("to", "target@bm.lan");

		map.put("message", "gg");

		ElasticsearchTestHelper.getInstance().getClient().prepareIndex("im", "im").setId(UUID.randomUUID().toString())
				.setSource(map).execute().actionGet();
		ESearchActivator.refreshIndex("im");

		getService(sysContext).getLastMessagesBetween("test@bm.lan", "target@bm.lan", 20);
	}

}
