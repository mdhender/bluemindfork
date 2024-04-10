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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class LoginTests {

	private int COUNT = 100;
	private final int PORT = 1143;
	private String loginUid;
	private String domainUid;

	@BeforeEach
	public void setUp() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		System.out.println("ES is " + esServer.ip);
		assertNotNull(esServer.ip);
		esServer.tags = Lists.newArrayList(TagDescriptor.bm_es.getTag());

		Server pipo = new Server();
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;
		pipo.tags = Lists.newArrayList(TagDescriptor.mail_imap.getTag());

		PopulateHelper.initGlobalVirt(pipo, esServer);
		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.none);

		domainUid = "test.devenv";
		loginUid = "user" + System.currentTimeMillis();
		PopulateHelper.addDomain(domainUid);
		PopulateHelper.addUser(loginUid, domainUid);
	}

	@AfterEach
	public void tearDown() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testSIDLoginLogout() throws Exception {

		IAuthentication authService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IAuthentication.class);

		LoginResponse resp = authService.login("admin0@global.virt", "admin", "testSIDLoginLogout");

		String sid = resp.authKey;
		assertNotNull(sid);

		try (StoreClient sc = new StoreClient("127.0.0.1", PORT, loginUid + "@" + domainUid, loginUid)) {
			boolean ok = sc.login();
			assertTrue(ok);
		} catch (Exception e) {
			e.printStackTrace();
			fail("error on login");
		} finally {
			JdbcTestHelper.getInstance().afterTest();
		}
	}

	@Test
	public void testLoginLogoutSpeed() {
		long time = System.nanoTime();
		for (int i = 0; i < COUNT; i++) {
			try (StoreClient sc = new StoreClient("127.0.0.1", PORT, loginUid + "@" + domainUid, domainUid)) {

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
