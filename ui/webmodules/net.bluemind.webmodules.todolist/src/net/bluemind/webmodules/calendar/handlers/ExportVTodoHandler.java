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
package net.bluemind.webmodules.calendar.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.streams.Pump;
import io.vertx.core.streams.ReadStream;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.Stream;
import net.bluemind.core.container.api.IContainerManagementAsync;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.ILocator;
import net.bluemind.core.rest.http.VertxServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.network.topology.Topology;
import net.bluemind.todolist.api.IVTodoAsync;
import net.bluemind.webmodule.server.NeedVertx;

public class ExportVTodoHandler implements Handler<HttpServerRequest>, NeedVertx {

	private static final Logger logger = LoggerFactory.getLogger(ExportVTodoHandler.class);
	private HttpClientProvider httpClientProvider;

	@Override
	public void handle(final HttpServerRequest request) {
		final String containerUid = request.params().get("containerUid");

		logger.debug("export vtodo for container {}", containerUid);
		final VertxServiceProvider clientProvider = getProvider(request);

		IContainerManagementAsync container = clientProvider.instance("bm/core", IContainerManagementAsync.class,
				containerUid);

		container.getDescriptor(new AsyncHandler<ContainerDescriptor>() {

			@Override
			public void success(ContainerDescriptor descriptor) {
				logger.debug("export vtodo for container {}", descriptor);

				if (descriptor == null) {
					HttpServerResponse resp = request.response();
					resp.setStatusCode(400);
					resp.end("No vtodo container for {}", containerUid);
					return;
				}
				request.response().headers().add("Content-Disposition",
						"attachment; filename=\"" + descriptor.name + "_" + containerUid + ".ics\"");

				request.response().setChunked(true);
				doExport(request, clientProvider, descriptor);

			}

			@Override
			public void failure(Throwable e) {
				logger.error("error retrieving todo container {}", containerUid, e);
				HttpServerResponse resp = request.response();
				resp.setStatusCode(500);
				resp.end();
			}

		});

	}

	protected void doExport(HttpServerRequest request, VertxServiceProvider clientProvider,
			ContainerDescriptor descriptor) {
		IVTodoAsync vcardService = clientProvider.instance("bm/core", IVTodoAsync.class, descriptor.uid);
		vcardService.exportAll(handleResponse(request, descriptor.uid));

	}

	private AsyncHandler<Stream> handleResponse(final HttpServerRequest request, final String containerUid) {
		return new AsyncHandler<Stream>() {

			@Override
			public void success(Stream exportStream) {
				stream(VertxStream.read(exportStream), request);
			}

			@Override
			public void failure(Throwable e) {
				logger.error("error during export VTodo of  {}", containerUid, e);
				HttpServerResponse resp = request.response();
				resp.setStatusCode(500);
				resp.end();
			}

		};
	}

	protected void stream(ReadStream<Buffer> exportStream, final HttpServerRequest request) {
		exportStream.endHandler(new Handler<Void>() {

			@Override
			public void handle(Void arg0) {
				request.response().setStatusCode(200);
				request.response().end();
			}
		});

		Pump.pump(exportStream, request.response()).start();

	}

	@Override
	public void setVertx(Vertx vertx) {
		httpClientProvider = new HttpClientProvider(vertx);
	}

	private static final ILocator locator = (String service, AsyncHandler<String[]> asyncHandler) -> {
		String core = Topology.get().core().value.address();
		String[] resp = new String[] { core };
		asyncHandler.success(resp);
	};

	private VertxServiceProvider getProvider(HttpServerRequest request) {

		String apiKey = request.headers().get("BMSessionId");
		return new VertxServiceProvider(httpClientProvider, locator, apiKey).from(request);

	}

}
