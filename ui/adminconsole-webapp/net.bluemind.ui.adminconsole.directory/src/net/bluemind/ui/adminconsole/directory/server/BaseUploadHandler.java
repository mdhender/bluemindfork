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
package net.bluemind.ui.adminconsole.directory.server;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.ILocator;
import net.bluemind.core.rest.http.VertxServiceProvider;
import net.bluemind.network.topology.Topology;
import net.bluemind.webmodule.server.NeedVertx;
import net.bluemind.webmodule.uploadhandler.TemporaryUploadRepository;

public abstract class BaseUploadHandler implements Handler<HttpServerRequest>, NeedVertx {

	protected Vertx vertx;
	protected TemporaryUploadRepository repository;
	protected HttpClientProvider clientProvider;

	@Override
	public final void setVertx(Vertx vertx) {
		this.vertx = vertx;
		this.clientProvider = new HttpClientProvider(vertx);
		this.repository = new TemporaryUploadRepository(vertx);
	}

	private static final ILocator locator = (String service, AsyncHandler<String[]> asyncHandler) -> {
		String core = Topology.get().core().value.address();
		String[] resp = new String[] { core };
		asyncHandler.success(resp);
	};

	protected final VertxServiceProvider getProvider(HttpServerRequest request) {

		String apiKey = request.headers().get("BMSessionId");
		return new VertxServiceProvider(clientProvider, locator, apiKey).from(request);

	}
}
