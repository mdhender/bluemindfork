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
package net.bluemind.webmodules.contact.handlers;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.addressbook.api.IVCardServiceAsync;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.ILocator;
import net.bluemind.core.rest.http.VertxServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.network.topology.Topology;
import net.bluemind.webmodule.server.NeedVertx;

public class ImportVCardHandler implements Handler<HttpServerRequest>, NeedVertx {

	private HttpClientProvider clientProvider;

	@Override
	public void handle(final HttpServerRequest request) {

		request.exceptionHandler(errorHandler(request));
		request.setExpectMultipart(true);
		request.uploadHandler(new Handler<HttpServerFileUpload>() {

			@Override
			public void handle(HttpServerFileUpload upload) {

				final Buffer data = Buffer.buffer();
				upload.handler(new Handler<Buffer>() {

					@Override
					public void handle(Buffer event) {
						data.appendBuffer(event);
					}
				});
				upload.endHandler(new Handler<Void>() {

					@Override
					public void handle(Void event) {
						doImport(request, data.toString());
					}

				});
			}
		});
	}

	private Handler<Throwable> errorHandler(final HttpServerRequest request) {
		return new Handler<Throwable>() {

			@Override
			public void handle(Throwable e) {
				HttpServerResponse resp = request.response();
				resp.setStatusCode(500);
				resp.setStatusMessage(e.getMessage() != null ? e.getMessage() : "null");
				resp.end();

			}
		};
	}

	private void doImport(final HttpServerRequest request, String vcard) {

		String containerUid = request.params().get("containerUid");

		VertxServiceProvider provider = getProvider(request);
		IVCardServiceAsync service = provider.instance("bm/core", IVCardServiceAsync.class, containerUid);

		service.importCards(vcard, new AsyncHandler<TaskRef>() {

			@Override
			public void success(TaskRef value) {
				HttpServerResponse resp = request.response();
				resp.setStatusCode(200);
				resp.end(value.id);
			}

			@Override
			public void failure(Throwable e) {
				HttpServerResponse resp = request.response();
				resp.setStatusCode(500);
				resp.setStatusMessage(e.getMessage() != null ? e.getMessage() : "null");
				resp.end();
			}

		});
	}

	@Override
	public void setVertx(Vertx vertx) {
		this.clientProvider = new HttpClientProvider(vertx);
	}

	private static final ILocator locator = (String service, AsyncHandler<String[]> asyncHandler) -> {
		String core = Topology.get().core().value.address();
		String[] resp = new String[] { core };
		asyncHandler.success(resp);
	};

	private VertxServiceProvider getProvider(HttpServerRequest request) {

		String apiKey = request.headers().get("BMSessionId");
		return new VertxServiceProvider(clientProvider, locator, apiKey).from(request);

	}
}
