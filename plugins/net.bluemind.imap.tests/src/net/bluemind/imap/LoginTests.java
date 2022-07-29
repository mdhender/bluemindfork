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
package net.bluemind.imap;

import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;

import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class LoginTests extends IMAPTestCase {

	private int COUNT = 100;
	private int PORT = 1143;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testSIDLoginLogout() throws Exception {

		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
		String ip = ElasticsearchTestHelper.getInstance().getHost();
		Server esServer = new Server();
		esServer.ip = ip;
		esServer.tags = Lists.newArrayList("bm/es");

		String cyrusIp = new BmConfIni().get("imap-role");
		Server imapServer = new Server();
		imapServer.ip = cyrusIp;
		imapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(esServer, imapServer);
		PopulateHelper.addDomainAdmin("admin0", "global.virt");

		IAuthentication authService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IAuthentication.class);

		LoginResponse resp = authService.login("admin0@global.virt", "admin", "testSIDLoginLogout");

		String sid = resp.authKey;
		assertNotNull(sid);

		try (StoreClient sc = new StoreClient(cyrusIp, PORT, "admin0", sid)) {
			boolean ok = sc.login();
			assertTrue(ok);
		} catch (Exception e) {
			e.printStackTrace();
			fail("error on login");
		} finally {
			JdbcTestHelper.getInstance().afterTest();
		}
	}

	public void testAdmin0TokenLoginLogout() throws ServerFault {
		try (StoreClient sc = new StoreClient(cyrusIp, PORT, "admin0", Token.admin0())) {
			boolean ok = sc.login();
			assertTrue(ok);
		} catch (Exception e) {
			e.printStackTrace();
			fail("error on login");
		}
	}

	public void testLoginLogoutSpeed() throws IMAPException, InterruptedException {
		long time = System.nanoTime();
		for (int i = 0; i < COUNT; i++) {
			try (StoreClient sc = new StoreClient(cyrusIp, PORT, testLogin, testPass)) {

				sc.logout();

			} catch (Exception e) {
				e.printStackTrace();
				fail("error on login");
			}
		}
		time = System.nanoTime() - time;
		if (time / COUNT == 0) {
			System.err.println("too fast..");
			return;
		}
		System.out.println(COUNT + " iterations in " + (time / 1000000) + "ms. " + ((time / 1000000) / COUNT)
				+ "ms avg, " + COUNT / (time / COUNT) + " per nanosec.");

	}
}
