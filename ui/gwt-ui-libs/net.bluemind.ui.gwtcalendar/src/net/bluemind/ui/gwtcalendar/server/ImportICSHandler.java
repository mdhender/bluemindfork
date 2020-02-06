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
package net.bluemind.ui.gwtcalendar.server;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerFileUpload;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.calendar.api.IVEventAsync;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.rest.base.GenericStream;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.VertxServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.locator.vertxclient.VertxLocatorClient;
import net.bluemind.webmodule.server.NeedVertx;

public class ImportICSHandler implements Handler<HttpServerRequest>, NeedVertx {

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

	private void doImport(final HttpServerRequest request, String ics) {

		String containerUid = request.params().get("calendar");

		VertxServiceProvider provider = getProvider(request);
		IVEventAsync service = provider.instance("bm/core", IVEventAsync.class, containerUid);

		service.importIcs(GenericStream.simpleValue(ics, i -> i.getBytes()), new AsyncHandler<TaskRef>() {

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

	public void setVertx(Vertx vertx) {
		this.clientProvider = new HttpClientProvider(vertx);
	}

	private VertxServiceProvider getProvider(HttpServerRequest request) {

		String login = request.headers().get("BMUserLogin");
		String apiKey = request.headers().get("BMSessionId");
		return new VertxServiceProvider(clientProvider, new VertxLocatorClient(clientProvider, login), apiKey)
				.from(request);

	}

}
