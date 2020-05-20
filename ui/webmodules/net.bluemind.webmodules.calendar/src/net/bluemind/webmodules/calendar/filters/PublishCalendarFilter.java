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
package net.bluemind.webmodules.calendar.filters;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.streams.Pump;
import io.vertx.core.streams.ReadStream;
import net.bluemind.calendar.api.IPublishCalendarAsync;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.ILocator;
import net.bluemind.core.rest.http.VertxServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.network.topology.Topology;
import net.bluemind.webmodule.server.IWebFilter;
import net.bluemind.webmodule.server.NeedVertx;

public class PublishCalendarFilter implements IWebFilter, NeedVertx {

	private static final Logger logger = LoggerFactory.getLogger(PublishCalendarFilter.class);
	private HttpClientProvider clientProvider;

	@Override
	public void setVertx(Vertx vertx) {
		this.clientProvider = new HttpClientProvider(vertx);
	}

	private static final ILocator locator = (String service, AsyncHandler<String[]> asyncHandler) -> {
		String core = Topology.get().core().value.address();
		String[] resp = new String[] { core };
		asyncHandler.success(resp);
	};

	@Override
	public CompletableFuture<HttpServerRequest> filter(HttpServerRequest request) {
		String path = request.path();
		if (!path.startsWith("/cal/calendar/publish")) {
			return CompletableFuture.completedFuture(request);
		}

		// request /cal/calendar/publish/container/token
		String req = request.path();
		try {
			req = URLDecoder.decode(req, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage(), e);
		}
		String[] params = req.split("/");
		String container = params[4];

		VertxServiceProvider provider = new VertxServiceProvider(clientProvider, locator, null).from(request);

		provider.instance("bm/core", IPublishCalendarAsync.class, container).publish(params[5], handler(request));

		return CompletableFuture.completedFuture(null);
	}

	private AsyncHandler<Stream> handler(final HttpServerRequest request) {
		return new AsyncHandler<Stream>() {
			@Override
			public void success(Stream value) {
				HttpServerResponse resp = request.response();
				resp.headers().set("Content-Type", "text/calendar;charset=UTF-8");
				resp.headers().set("Content-Disposition", "attachment; filename=\"calendar.ics\"");
				ReadStream<Buffer> read = VertxStream.read(value);
				Pump pump = Pump.pump(read, resp);
				resp.setChunked(true);
				pump.start();
				read.endHandler(v -> resp.end());
			}

			@Override
			public void failure(Throwable e) {

				int statusCode = 500;
				if (e instanceof ServerFault) {
					ErrorCode errCode = ((ServerFault) e).getCode();
					if (errCode == ErrorCode.NOT_FOUND) {
						statusCode = 404;
					} else if (errCode == ErrorCode.PERMISSION_DENIED) {
						statusCode = 403;
					}
				}

				request.response().setStatusCode(statusCode);
				request.response().end();

			}
		};
	}

}
