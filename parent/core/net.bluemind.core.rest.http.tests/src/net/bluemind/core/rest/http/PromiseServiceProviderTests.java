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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.core.rest.http;

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.Vertx;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.rest.tests.services.ComplexRequest;
import net.bluemind.core.rest.tests.services.ComplexResponse;
import net.bluemind.core.rest.tests.services.IRestTestServicePromise;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.server.api.TagDescriptor;

public class PromiseServiceProviderTests {

	private ILocator locator;

	@Before
	public void before() throws Exception {
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
		this.locator = new ILocator() {

			@Override
			public void locate(String service, AsyncHandler<String[]> asyncHandler) {
				asyncHandler.success(new String[] { "127.0.0.1" });
			}
		};
	}

	@After
	public void after() throws Exception {
		this.locator = null;
	}

	@Test
	public void testGetPromiseApi() throws InterruptedException, ExecutionException, TimeoutException {
		Vertx vertx = VertxPlatform.getVertx();
		HttpClientProvider prov = new HttpClientProvider(vertx);
		VertxPromiseServiceProvider sp = new VertxPromiseServiceProvider(prov, locator, null);
		IRestTestServicePromise promiseProxy = sp.instance(TagDescriptor.bm_core.getTag(),
				IRestTestServicePromise.class);
		assertNotNull(promiseProxy);
		ComplexRequest cr = new ComplexRequest();
		CompletableFuture<ComplexResponse> promise = promiseProxy.complex(cr);
		assertNotNull(promise);
		ComplexResponse complexResponse = promise.get(1, TimeUnit.SECONDS);
		assertNotNull(complexResponse);
	}

}
