/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.metrics.core.tests;

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.core.api.Stream;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.metrics.alerts.api.IAlerts;

public class AlertsServiceTests {

	private String apiKey;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		CountDownLatch cdl = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(ar -> cdl.countDown());
		cdl.await();
		this.apiKey = "yeah-yeah";
		Sessions.get().put(apiKey, SecurityContext.SYSTEM);
	}

	@Test
	public void testAlertsService() throws InterruptedException {
		ClientSideServiceProvider prov = ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", apiKey);
		IAlerts alertsApi = prov.instance(IAlerts.class);
		assertNotNull(alertsApi);
		JsonObject payload = new JsonObject().putString("alerte", "générale");
		Stream payloadStream = VertxStream.stream(new Buffer(payload.encode().getBytes()));
		CountDownLatch cdl = new CountDownLatch(1);
		VertxPlatform.eventBus().registerHandler("kapacitor.alert", (Message<JsonObject> msg) -> {
			cdl.countDown();
		});
		alertsApi.receive(payloadStream);
		cdl.await(3, TimeUnit.SECONDS);
	}

}
