/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.webmodule.server.export;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.streams.ReadStream;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.Stream;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.rest.http.ILocator;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.network.topology.Topology;

public class ExportHelper {

	private static final Logger logger = LoggerFactory.getLogger(ExportHelper.class);

	public static final ILocator locator = (String service, AsyncHandler<String[]> asyncHandler) -> {
		String core = Topology.get().core().value.address();
		String[] resp = new String[] { core };
		asyncHandler.success(resp);
	};

	public static ILocator locator() {
		return locator;
	}

	public static Void setResponse(HttpServerRequest request, String type, ContainerDescriptor containerDescriptor,
			Stream ics) {
		String date = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm"));
		String filename = String.format("%s-bluemind-%s-export-%s.ics", containerDescriptor.name, type, date);

		HttpServerResponse resp = request.response();
		String ua = request.headers().get("User-Agent");
		resp.headers().set("Content-Type", "text/calendar;charset=UTF-8");
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
		resp.setChunked(true);
		read.pipeTo(resp);
		return null;

	}

	public static Void error(final HttpServerResponse response, String container, Throwable e) {
		logger.error("error ics export of {} ", container, e);
		response.setStatusCode(500);
		response.setStatusMessage(e.getMessage());
		response.end();
		return null;
	}

}
