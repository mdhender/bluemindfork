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

import java.io.File;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.ILocator;
import net.bluemind.core.rest.http.VertxServiceProvider;
import net.bluemind.network.topology.Topology;
import net.bluemind.webmodule.server.NeedVertx;
import net.bluemind.webmodule.uploadhandler.TemporaryUploadRepository;

public abstract class BaseUploadHandler<T> implements Handler<HttpServerRequest>, NeedVertx {

	private static Logger logger = LoggerFactory.getLogger(BaseUploadHandler.class);

	protected Vertx vertx;
	protected TemporaryUploadRepository repository;
	protected HttpClientProvider clientProvider;

	@Override
	public void handle(final HttpServerRequest request) {
		final String domainUid = request.params().get("domainUid");
		final String entityId = request.params().get(entityIdParameter());
		String iconId = request.params().get("iconId");
		UUID parsed = null;
		try {
			parsed = UUID.fromString(iconId);
		} catch (IllegalArgumentException e) {
			request.response().setStatusCode(500).end();
			return;
		}
		if (domainUid == null || entityId == null || iconId == null) {
			request.response().setStatusCode(500);
			request.response().setStatusMessage("bad paramters");
			request.response().end();
			return;
		}

		File file = repository.getTempFile(parsed);
		if (file == null || !file.exists()) {
			request.response().setStatusCode(500);
			request.response().setStatusMessage("temp file doesnt exists anymore");
			request.response().end();
			return;
		}

		vertx.fileSystem().readFile(file.getPath(), new Handler<AsyncResult<Buffer>>() {

			@Override
			public void handle(AsyncResult<Buffer> event) {
				doSetIcon(request, domainUid, entityId, event.result().getBytes());
			}
		});
	}

	protected void doSetIcon(final HttpServerRequest request, final String domainUid, final String entityId,
			byte[] data) {
		T entityService = entityService(request, domainUid);
		setUploadData(entityService, entityId, data, new AsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				request.response().setStatusCode(200);
				request.response().end();
			}

			@Override
			public void failure(Throwable e) {
				logger.error("error during set image to resource type {}:{} : {}", domainUid, entityId, e.getMessage());
				request.response().setStatusCode(500);
				request.response().setStatusMessage(e.getMessage() != null ? e.getMessage() : "null");
				request.response().end();
			}
		});
	}

	protected abstract String entityIdParameter();

	protected abstract T entityService(final HttpServerRequest request, final String domainUid);

	protected abstract void setUploadData(T entityService, String entityId, byte[] data, AsyncHandler<Void> handler);

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
