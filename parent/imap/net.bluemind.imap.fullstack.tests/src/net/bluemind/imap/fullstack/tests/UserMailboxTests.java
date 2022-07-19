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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.imap.fullstack.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.imap.ListResult;
import net.bluemind.imap.StoreClient;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.server.api.Server;
import net.bluemind.system.state.RunningState;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class UserMailboxTests {

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		Server pipo = new Server();
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;
		pipo.tags = Collections.singletonList("mail/imap");

		VertxPlatform.spawnBlocking(25, TimeUnit.SECONDS);

		PopulateHelper.initGlobalVirt(pipo);
		String domUid = "devenv.blue";
		PopulateHelper.addDomain(domUid, Routing.internal);
		String userUid = PopulateHelper.addUser("john", "devenv.blue", Routing.internal);
		assertNotNull(userUid);

		StateContext.setInternalState(new RunningState());
		System.err.println("==== BEFORE ====");

	}

	@After
	public void after() throws Exception {
		System.err.println("===== AFTER =====");
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void userCanConnect() throws Exception {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1144, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			for (int i = 0; i < 10; i++) {
				Thread.sleep(250);
				sc.noop();
			}

		}
	}

	@Test
	public void defaultFoldersExist() throws Exception {

		try (StoreClient sc = new StoreClient("127.0.0.1", 1144, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			ListResult userFolders = sc.listAll();
			assertFalse("user folders list must not be empty", userFolders.isEmpty());
		}
	}

}
