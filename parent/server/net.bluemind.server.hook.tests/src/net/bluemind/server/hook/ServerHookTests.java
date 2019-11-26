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
package net.bluemind.server.hook;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.config.InstallationId;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class ServerHookTests {

	private Container installation;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		ContainerStore containerHome = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM);

		String instUid = InstallationId.getIdentifier();
		installation = Container.create(instUid, "installation", instUid, "me", true);
		installation = containerHome.create(installation);
		assertNotNull(installation);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testHooksAreCalled() throws Exception {

		IServer serverService = getService();
		String uid = "prec";
		Server srv = defaultServer();
		serverService.create(uid, srv);
		srv.tags = Lists.newArrayList("un/used", "ro/co");
		serverService.update(uid, srv);
		serverService.delete(uid);
		assertTrue("Hooks not called in the 15sec window: " + TestHook.latch.getCount(),
				TestHook.latch.await(15, TimeUnit.SECONDS));
	}

	private Server defaultServer() {
		Server s = new Server();
		s.fqdn = new BmConfIni().get("host");
		s.ip = s.fqdn;
		s.name = s.fqdn;
		s.tags = Lists.newArrayList("john/bang");
		return s;
	}

	private IServer getService() throws Exception {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class, installation.uid);
	}

}
