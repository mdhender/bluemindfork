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

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import com.google.common.collect.Lists;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.imap.vertx.VXStoreClient;
import net.bluemind.imap.vertx.connection.NetClientConnectionSupport;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.stateobserver.testhelper.StateTestHelper;
import net.bluemind.tests.defaultdata.PopulateHelper;

public abstract class WithMailboxTests {

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("node.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP + "," + PopulateHelper.FAKE_CYRUS_IP_2);
		System.setProperty("imap.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP + "," + PopulateHelper.FAKE_CYRUS_IP_2);
		System.setProperty("ahcnode.fail.https.ok", "true");
	}

	@BeforeClass
	public static void afterClass() {
		System.clearProperty("node.local.ipaddr");
		System.clearProperty("imap.local.ipaddr");
		System.clearProperty("ahcnode.fail.https.ok");
	}

	protected String mailbox;
	protected String domain;
	protected String localPart;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		VertxPlatform.spawnBlocking(10, TimeUnit.SECONDS);

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		System.out.println("ES is " + esServer.ip);
		assertNotNull(esServer.ip);
		esServer.tags = Lists.newArrayList(TagDescriptor.bm_es.getTag());

		Server pipo = new Server();
		pipo.tags = Collections.singletonList(TagDescriptor.mail_imap.getTag());
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;

		PopulateHelper.initGlobalVirt(pipo, esServer);

		ElasticsearchTestHelper.getInstance().beforeTest();

		StateTestHelper.blockUntilRunning().orTimeout(30, TimeUnit.SECONDS).join();

		this.domain = "test" + System.currentTimeMillis() + ".lab";
		this.localPart = "u" + System.currentTimeMillis();
		this.mailbox = "user/" + localPart + "@" + domain;
		Mailbox mb = new Mailbox();
		mb.routing = Routing.internal;
		mb.type = Type.user;
		mb.name = localPart;
		mb.dataLocation = PopulateHelper.FAKE_CYRUS_IP;
		PopulateHelper.addDomain(domain);
		PopulateHelper.addUser(localPart, domain);
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
		NetClient client = vx.createNetClient(new NetClientOptions().setRegisterWriteHandler(true));
		NetClientConnectionSupport nccs = new NetClientConnectionSupport(vx, client);
		return new VXStoreClient(nccs, "127.0.0.1", 1143, localPart + "@" + domain, localPart);
	}

}