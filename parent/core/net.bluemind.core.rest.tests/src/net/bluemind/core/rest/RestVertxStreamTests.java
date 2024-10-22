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

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.tests.services.IRestStreamTestService;
import net.bluemind.core.rest.tests.services.IRestStreamTestServiceAsync;
import net.bluemind.core.rest.tests.services.RestStreamServiceTests;
import net.bluemind.core.rest.vertx.VertxEventBusClientFactory;
import net.bluemind.lib.vertx.VertxPlatform;

public class RestVertxStreamTests extends RestStreamServiceTests {

	@Before
	public void setup() throws Exception {
		super.before();
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
	}

	@After
	public void after() {
	}

	@Override
	protected IRestStreamTestService getService() {

		VertxEventBusClientFactory<IRestStreamTestService, IRestStreamTestServiceAsync> factory = new VertxEventBusClientFactory<>(
				IRestStreamTestService.class, IRestStreamTestServiceAsync.class, VertxPlatform.getVertx());
		return factory.syncClient(SecurityContext.ANONYMOUS);
	}
}
