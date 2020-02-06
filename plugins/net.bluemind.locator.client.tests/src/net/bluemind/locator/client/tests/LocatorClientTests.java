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
package net.bluemind.locator.client.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.domain.api.Domain;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.locator.client.LocatorClient;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class LocatorClientTests {

	private String testDomainUid;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		testDomainUid = "test.lan";

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		PopulateHelper.initGlobalVirt(esServer);

		ItemValue<Domain> domain = PopulateHelper.createTestDomain(testDomainUid);
		PopulateHelper.addUser("test", domain.uid);
		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});
		launched.await();
	}

	@Test
	public void testGlobalLocate() {
		LocatorClient lc = new LocatorClient();
		assertNotNull(lc);
		lc.locateHost("bm/core", "admin0@global.virt");
	}

	@Test
	public void testGlobalLocateSpeed() {
		LocatorClient lc = new LocatorClient();
		assertNotNull(lc);
		int CNT = 10000;
		long time = System.currentTimeMillis();
		for (int i = 0; i < CNT; i++) {
			lc.locateHost("bm/core", "admin0@global.virt");
		}
		time = System.currentTimeMillis() - time;
		int perSec = (CNT * 1000) / (int) time;
		System.err.println("For " + CNT + ": " + time + "ms, performing " + perSec + "/sec");
	}

	@Test
	public void testGlobalLocateSpeedThreaded() throws InterruptedException, ExecutionException {
		final LocatorClient lc = new LocatorClient();
		assertNotNull(lc);
		int CNT = 100000;
		long time = System.currentTimeMillis();
		Runnable r = new Runnable() {

			@Override
			public void run() {
				lc.locateHost("bm/core", "admin0@global.virt");
			}
		};
		ExecutorService pool = Executors.newFixedThreadPool(4);
		for (int i = 0; i < CNT; i++) {
			pool.execute(r);
		}
		pool.shutdown();
		boolean beforeTimeout = pool.awaitTermination(1, TimeUnit.MINUTES);
		assertTrue("Waiting for termination timed out", beforeTimeout);
		time = System.currentTimeMillis() - time;
		int perSec = (CNT * 1000) / (int) time;
		System.err.println("For " + CNT + ": " + time + "ms, performing " + perSec + "/sec");
	}

	@Test
	public void testNotFoundSpeed() {
		LocatorClient lc = new LocatorClient();
		assertNotNull(lc);
		int CNT = 512;
		long time = System.currentTimeMillis();
		for (int i = 0; i < CNT; i++) {
			lc.locateHost("bm/coreNotFound", "admin@buffy.kvm");
			// System.err.println("Iteration " + i + ": " + ret);
		}
		time = System.currentTimeMillis() - time;
		int perSec = (CNT * 1000) / (int) time;
		System.err.println("For " + CNT + ": " + time + "ms, performing " + perSec + "/sec");
	}

}
