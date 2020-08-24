/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.node.metrics.aggregator.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.node.metrics.aggregator.MetricsTcpAggregatorVerticle;
import net.bluemind.node.metrics.aggregator.SystemProps;
import net.bluemind.vertx.testhelper.Deploy;

public class AggregatedMetricsTests {

	private Path sockDir;
	private Set<String> dep;

	@Before
	public void before() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		this.sockDir = Files.createTempDirectory("metrics");

		System.setProperty(SystemProps.SOCKET_DIR_PROP, sockDir.toFile().getAbsolutePath());

		this.dep = Deploy.verticles(false, MetricsTcpAggregatorVerticle::new, FakeMetricsSockets::new).get(10,
				TimeUnit.SECONDS);
	}

	@After
	public void after() {
		Deploy.afterTest(dep);
		this.sockDir.toFile().delete();
	}

	@Test
	public void readAggregatedMetrics() throws InterruptedException, ExecutionException, TimeoutException {
		try (AsyncHttpClient ahc = new DefaultAsyncHttpClient()) {
			String body = ahc.prepareGet("http://127.0.0.1:8019/metrics").execute().get(5, TimeUnit.SECONDS)
					.getResponseBody();
			System.err.println("BODY:\n" + body);
			assertNotNull(body);
			assertTrue(body.contains("bm-core.hprof"));
			assertTrue(body.contains("ysnp.hprof"));
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
