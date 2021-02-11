/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.imap.vertx.tests;

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;

import com.google.common.collect.Lists;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.net.NetClient;
import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.imap.vertx.VXStoreClient;
import net.bluemind.imap.vertx.con.EventBusConnectionSupport;
import net.bluemind.imap.vertx.con.NetClientConnectionSupport;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.pool.impl.docker.DockerContainer;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public abstract class WithMailboxTests {

	protected String imapIp;
	protected String mailbox;
	protected String domain;
	protected String localPart;
	protected CyrusService cyrus;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		BmConfIni ini = new BmConfIni();
		imapIp = ini.get(DockerContainer.IMAP.getHostProperty());

		VertxPlatform.spawnBlocking(10, TimeUnit.SECONDS);

		assertNotNull(imapIp);
		Server imapServer = new Server();
		imapServer.ip = imapIp;
		imapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(imapServer);

		this.cyrus = new CyrusService(imapIp);
		this.domain = "test" + System.currentTimeMillis() + ".lab";
		cyrus.createPartition(domain);
		cyrus.refreshPartitions(Arrays.asList(domain));
		cyrus.refreshAnnotations();
		cyrus.reload();
		this.localPart = "u" + System.currentTimeMillis();
		this.mailbox = "user/" + localPart + "@" + domain;
		cyrus.createBox(mailbox, domain);

	}

	@After
	public void after() {
	}

	/**
	 * this one uses {@link VertxPlatform#getVertx()}, prefer {@link #client(Vertx)}
	 * if you have a valid instance in hand.
	 * 
	 * @return a client
	 */
	protected VXStoreClient client() {
		return client(VertxPlatform.getVertx());
	}

	protected VXStoreClient client(Vertx vx) {
		NetClient client = vx.createNetClient();
		NetClientConnectionSupport nccs = new NetClientConnectionSupport(client);
		return new VXStoreClient(nccs, imapIp, 1143, localPart + "@" + domain, "gg");
	}

	protected VXStoreClient eventBusClient(Vertx vx) {
		EventBus eb = vx.eventBus();
		EventBusConnectionSupport nccs = new EventBusConnectionSupport(eb);
		return new VXStoreClient(nccs, imapIp, 1143, localPart + "@" + domain, "gg");
	}

}
