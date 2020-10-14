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
package net.bluemind.backend.mail.replica.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.LongAdder;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.locator.LocatorVerticle;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.network.topology.Topology;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.vertx.testhelper.Deploy;

public class LocatorVerticleTests {

	private String cyrusIp;
	private String esIp;
	private String domainUid;
	private static final String prefix = "http://127.0.0.1:8084/location/host/";

	/**
	 * login local part == uid for unit tests
	 */
	private String userUid;

	private String uniqueUidPart() {
		return System.currentTimeMillis() + "";
	}

	@Before
	public void before() throws Exception {

		JdbcTestHelper.getInstance().beforeTest();
		Deploy.verticles(false, LocatorVerticle::new).get(5, TimeUnit.SECONDS);

		BmConfIni ini = new BmConfIni();

		Server esServer = new Server();
		this.esIp = ElasticsearchTestHelper.getInstance().getHost();

		esServer.ip = esIp;
		assertNotNull(esServer.ip);
		esServer.tags = Lists.newArrayList("bm/es");

		this.cyrusIp = ini.get("imap-role");
		Server imapServer = new Server();
		imapServer.ip = cyrusIp;
		imapServer.tags = Lists.newArrayList("mail/imap");

		ItemValue<Server> cyrusServer = ItemValue.create("cyrus-container", imapServer);
		CyrusService cyrusService = new CyrusService(cyrusServer);
		cyrusService.reset();

		PopulateHelper.initGlobalVirt(esServer, imapServer);
		ElasticsearchTestHelper.getInstance().beforeTest();
		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.none);

		String unique = uniqueUidPart();
		domainUid = "test" + unique + ".lab";
		userUid = "user" + unique;

		// ensure the partition is created correctly before restarting cyrus
		PopulateHelper.addDomain(domainUid, Routing.none);

		JdbcActivator.getInstance().addMailboxDataSource(cyrusServer.uid,
				JdbcTestHelper.getInstance().getMailboxDataDataSource());

		CountDownLatch cdl = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(ar -> {
			cdl.countDown();
		});
		boolean beforeTimeout = cdl.await(30, TimeUnit.SECONDS);
		assertTrue(beforeTimeout);

		MQ.init().get(30, TimeUnit.SECONDS);
		Topology.get();

		System.err.println("Start populate user " + userUid);
		PopulateHelper.addUser(userUid, domainUid, Routing.internal);
	}

	@After
	public void after() throws Exception {
		System.err.println("test is over, time for after()");
		JdbcTestHelper.getInstance().afterTest();
	}

	/**
	 * We test from here because locator-client is gone & we want a 'complex'
	 * topology for LocatorVerticle coverage
	 * 
	 * Locator code is still called by roundcube so we need it in a working state.
	 * 
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	@Test
	public void testLocateOverHttp() throws InterruptedException, ExecutionException, TimeoutException, IOException {
		// rm.get("/location/host/:kind/:tag/:latd", hls)
		LongAdder topo = new LongAdder();
		LongAdder sql = new LongAdder();
		try (AsyncHttpClient ahc = new DefaultAsyncHttpClient()) {
			locateWithTimings(ahc, topo, sql);
		}
		System.err.println("TOPO total: " + topo.sum() + "ms, SQL total: " + sql.sum() + "ms.");
	}

	private void locateWithTimings(AsyncHttpClient ahc, LongAdder topo, LongAdder sql)
			throws InterruptedException, ExecutionException, TimeoutException, IOException {
		assertTrue(Topology.getIfAvailable().isPresent());
		System.setProperty("locator.topology.disable", "false");
		long topoTime = System.currentTimeMillis();
		runLocationRequests(ahc);
		topoTime = System.currentTimeMillis() - topoTime;
		topo.add(topoTime);

		// force sql based location requests
		System.setProperty("locator.topology.disable", "true");
		long sqlTime = System.currentTimeMillis();
		runLocationRequests(ahc);
		sqlTime = System.currentTimeMillis() - sqlTime;
		sql.add(sqlTime);
	}

	@Test
	public void testLocatePerfTopoVersusSql()
			throws InterruptedException, ExecutionException, TimeoutException, IOException {
		int cnt = 1000;
		LongAdder topo = new LongAdder();
		LongAdder sql = new LongAdder();
		try (AsyncHttpClient ahc = new DefaultAsyncHttpClient()) {
			// warm up
			for (int i = 0; i < 50; i++) {
				locateWithTimings(ahc, topo, sql);
			}
			topo.reset();
			sql.reset();

			for (int i = 0; i < cnt; i++) {
				locateWithTimings(ahc, topo, sql);
			}
		}
		System.err.println("TOPO total: " + topo.sum() + "ms, SQL total: " + sql.sum() + "ms.");
		assertTrue("TOPOLOGY based location should be faster than SQL based location", topo.sum() < sql.sum());
	}

	private void runLocationRequests(AsyncHttpClient ahc)
			throws InterruptedException, ExecutionException, TimeoutException, IOException {
		Response userImapResp = ahc.prepareGet(prefix + "mail/imap" + "/" + userUid + "@" + domainUid).execute().get(2,
				TimeUnit.SECONDS);
		assertNotNull(userImapResp);
		assertEquals(cyrusIp, userImapResp.getResponseBody());

		Response ad0ImapResp = ahc.prepareGet(prefix + "mail/imap" + "/" + "admin0@global.virt").execute().get(2,
				TimeUnit.SECONDS);
		assertNotNull(ad0ImapResp);
		assertEquals(cyrusIp, ad0ImapResp.getResponseBody());

		Response userEsResp = ahc.prepareGet(prefix + "bm/es" + "/" + userUid + "@" + domainUid).execute().get(2,
				TimeUnit.SECONDS);
		assertNotNull(userEsResp);
		assertEquals(esIp, userEsResp.getResponseBody());

		Response ad0EsResp = ahc.prepareGet(prefix + "bm/es" + "/" + "admin0@global.virt").execute().get(2,
				TimeUnit.SECONDS);
		assertNotNull(ad0EsResp);
		assertEquals(esIp, ad0EsResp.getResponseBody());

		Response userCtiResp = ahc.prepareGet(prefix + "cti/frontend" + "/" + userUid + "@" + domainUid).execute()
				.get(2, TimeUnit.SECONDS);
		assertNotNull(userCtiResp);
		assertEquals(404, userCtiResp.getStatusCode());
	}

}
