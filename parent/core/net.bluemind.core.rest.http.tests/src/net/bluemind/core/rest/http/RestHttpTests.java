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
package net.bluemind.core.rest.http;

import java.util.concurrent.TimeUnit;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.After;
import org.junit.Before;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.tests.services.IRestPathTestService;
import net.bluemind.core.rest.tests.services.IRestPathTestServiceAsync;
import net.bluemind.core.rest.tests.services.IRestTestService;
import net.bluemind.core.rest.tests.services.IRestTestServiceAsync;
import net.bluemind.core.rest.tests.services.RestTestServiceTests;
import net.bluemind.lib.vertx.VertxPlatform;

public class RestHttpTests extends RestTestServiceTests {

	private AsyncHttpClient httpClient;

	@Before
	public void setup() throws Exception {

		VertxPlatform.spawnBlocking(20, TimeUnit.SECONDS);

		httpClient = new DefaultAsyncHttpClient();
	}

	@After
	public void after() throws Exception {
		httpClient.close();
	}

	@Override
	public IRestTestService getRestTestService(SecurityContext context) {
		return HttpClientFactory.create(IRestTestService.class, IRestTestServiceAsync.class, "http://localhost:8090")
				.syncClient(context.getSessionId());
	}

	@Override
	public IRestPathTestService getRestPathTestService(SecurityContext context, String param1, String param2) {
		return HttpClientFactory
				.create(IRestPathTestService.class, IRestPathTestServiceAsync.class, "http://localhost:8090")
				.syncClient(context.getSessionId(), param1, param2);
	}

}
