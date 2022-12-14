/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.directory.service.xfer.tests;

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import com.google.common.collect.Lists;

import net.bluemind.backend.mailapi.testhelper.MailApiTestsBase;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.api.TaskStatus.State;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.service.SplittedShardsMapping;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class AbstractMultibackendTests {

	protected ItemValue<Server> cyrusServer1;
	protected ItemValue<Server> cyrusServer2;

	protected String domainUid = "bm.lan";
	protected String userUid = "test" + System.currentTimeMillis();
	protected String shardIp;
	protected SecurityContext context;

	@BeforeClass
	public static void setXferTestMode() {
		MailApiTestsBase.beforeClass();
		System.setProperty("bluemind.testmode", "true");
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		System.out.println("ES is " + esServer.ip);
		assertNotNull(esServer.ip);
		esServer.tags = Lists.newArrayList("bm/es");

		Server imapServer = new Server();
		imapServer.ip = PopulateHelper.FAKE_CYRUS_IP;
		imapServer.tags = Lists.newArrayList("mail/imap", "bm/pgsql-data");
		cyrusServer1 = ItemValue.create(imapServer.ip, imapServer);

		Server imapServer2 = new Server();
		imapServer2.ip = PopulateHelper.FAKE_CYRUS_IP_2;
		imapServer2.tags = Lists.newArrayList("mail/imap", "bm/pgsql-data");
		cyrusServer2 = ItemValue.create(imapServer2.ip, imapServer2);

		Server pg2 = new Server();
		shardIp = new BmConfIni().get("pg2");
		pg2.ip = shardIp;
		pg2.tags = Lists.newArrayList("mail/shard");

		PopulateHelper.initGlobalVirt(pg2, esServer, imapServer, imapServer2);
		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.none);
		ElasticsearchTestHelper.getInstance().beforeTest();

		PopulateHelper.addDomain(domainUid, Routing.internal);

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		System.err.println("PG2 " + pg2.ip + " IMAP1: " + imapServer.ip + " IMAP2: " + imapServer2.ip);
		JdbcTestHelper.getInstance().initNewServer(pg2.ip);
		SplittedShardsMapping.map(pg2.ip, imapServer2.ip);

		PopulateHelper.addUser(userUid, domainUid, Routing.internal);

		context = new SecurityContext("user", userUid, Arrays.<String>asList(), Arrays.<String>asList(), domainUid);
		Sessions.get().put(context.getSessionId(), context);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	protected void waitTaskEnd(TaskRef taskRef) throws ServerFault {
		TaskStatus status = TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), taskRef);
		System.err.println("EndStatus: " + status);
		if (status.state == State.InError) {
			throw new ServerFault("xfer error");
		}
	}
}
