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
package net.bluemind.core.rest;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Before;

import io.vertx.core.Vertx;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.tests.services.IRestTestService;
import net.bluemind.core.rest.tests.services.IRestTestServiceAsync;
import net.bluemind.core.rest.tests.services.RestTestServiceTests;
import net.bluemind.core.rest.vertx.VertxEventBusClientFactory;
import net.bluemind.lib.vertx.VertxPlatform;

public class RestVerticleTests extends RestTestServiceTests {

	private Vertx eventBus;

	@Before
	public void setup() throws InterruptedException, ExecutionException {
		eventBus = VertxPlatform.getVertx();
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
	}

	@Override
	public IRestTestService getRestTestService(SecurityContext context) {

		VertxEventBusClientFactory<IRestTestService, IRestTestServiceAsync> factory = new VertxEventBusClientFactory<>(
				IRestTestService.class, IRestTestServiceAsync.class, eventBus);
		IRestTestService client = factory.syncClient(SecurityContext.ANONYMOUS);

		return client;
	}

}
