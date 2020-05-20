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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.streams.Pump;
import io.vertx.core.streams.ReadStream;
import net.bluemind.calendar.api.IVEventPromise;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.container.api.IContainersPromise;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.ILocator;
import net.bluemind.core.rest.http.VertxPromiseServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.network.topology.Topology;
import net.bluemind.webmodule.server.NeedVertx;

public class ExportICSHandler implements Handler<HttpServerRequest>, NeedVertx {

	private static Logger logger = LoggerFactory.getLogger(ExportICSHandler.class);
	private HttpClientProvider prov;

	@Override
	public void setVertx(Vertx vertx) {
		prov = new HttpClientProvider(vertx);
	}

	private static final ILocator locator = (String service, AsyncHandler<String[]> asyncHandler) -> {
		String core = Topology.get().core().value.address();
		String[] resp = new String[] { core };
		asyncHandler.success(resp);
	};

	@Override
	public void handle(final HttpServerRequest request) {
		String container = request.params().get("container");
		String latd = request.headers().get("BMUserLATD");
		String sessionId = request.headers().get("BMSessionId");

		final VertxPromiseServiceProvider clientProvider = new VertxPromiseServiceProvider(prov, locator, sessionId);

		IContainersPromise icp = clientProvider.instance(IContainersPromise.class);
		IVEventPromise ivep = clientProvider.instance(IVEventPromise.class, container);

		icp.get(container).thenCombine(ivep.exportAll(), (containerDescriptor, ics) -> {
			String date = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm"));
			String filename = String.format("%s-bluemind-export-%s.ics", containerDescriptor.name, date);

			HttpServerResponse resp = request.response();
			resp.headers().set("Content-Type", "text/calendar;charset=UTF-8");

			String ua = request.headers().get("User-Agent");
			if (ua.contains("firefox")) {
				resp.headers().set("Content-Disposition", "attachment; filename=*utf8''\"" + filename + "\"");
			} else if (ua.contains("msie")) {
				try {
					resp.headers().set("Content-Disposition",
							"attachment; filename=\"" + URLEncoder.encode(filename, "UTF-8") + "\"");
				} catch (UnsupportedEncodingException e) {
					logger.error(e.getMessage(), e);
				}
			} else {
				resp.headers().set("Content-Disposition", "attachment; filename=\"" + filename + "\"");
			}

			ReadStream<Buffer> read = VertxStream.read(ics);
			Pump pump = Pump.pump(read, resp);
			resp.setChunked(true);
			pump.start();
			read.endHandler((v) -> {
				resp.end();
			});

			return null;
		}).exceptionally(e -> {
			logger.error("error ics export of calendar {} ", container, e);
			request.response().setStatusCode(500);
			request.response().setStatusMessage(e.getMessage() != null ? e.getMessage() : "null");
			request.response().end();
			return null;
		});

	}

}
