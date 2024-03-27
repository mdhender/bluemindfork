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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;

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

public abstract class CyradmTestCase {
	private static final int PORT = 1143;
	protected StoreClient sc;
	protected String mboxCyrusPrefix = "user/";
	protected String mboxName = "u" + System.currentTimeMillis();
	protected String domainUid = "bm.lan";
	protected String mboxLogin = mboxName + "@" + domainUid;

	protected String mboxCyrusName = mboxCyrusPrefix + mboxName + "@" + domainUid;
	protected String loginUid;
	ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

	@Before
	public void setUp() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		Server pipo = new Server();
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;
		pipo.tags = Collections.singletonList(TagDescriptor.mail_imap.getTag());

		PopulateHelper.initGlobalVirt(pipo);

		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.none);

		ElasticsearchTestHelper.getInstance().beforeTest();

		domainUid = "test.devenv";
		loginUid = "user" + System.currentTimeMillis();
		PopulateHelper.addDomain(domainUid);
		PopulateHelper.addUser(loginUid, domainUid);

		DataSource datasource = JdbcTestHelper.getInstance().getMailboxDataDataSource();
		assertNotNull(datasource);
		ServerSideServiceProvider.mailboxDataSource.put("bm-master", datasource);

		sc = new StoreClient("127.0.0.1", PORT, loginUid + "@" + domainUid, loginUid);
		boolean login = sc.login();
		if (!login) {
			fail("login failed for " + loginUid);
		}

	}

	@After
	public void tearDown() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
		if (mboxCyrusName != null) {
			System.err.println("deleting mbox " + mboxCyrusName);
		}
		sc.logout();
	}
}
