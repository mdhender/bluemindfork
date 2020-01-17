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

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import net.bluemind.calendar.api.IPrintAsync;
import net.bluemind.calendar.api.PrintData;
import net.bluemind.calendar.api.PrintOptions;
import net.bluemind.calendar.api.PrintOptions.CalendarMetadata;
import net.bluemind.calendar.api.PrintOptions.PrintFormat;
import net.bluemind.calendar.api.PrintOptions.PrintLayout;
import net.bluemind.calendar.api.PrintOptions.PrintView;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.VertxServiceProvider;
import net.bluemind.locator.vertxclient.VertxLocatorClient;
import net.bluemind.webmodule.server.NeedVertx;

public class PrintCalendarHandler implements Handler<HttpServerRequest>, NeedVertx {

	private HttpClientProvider clientProvider;

	@Override
	public void handle(final HttpServerRequest request) {
		request.exceptionHandler(errorHandler(request));

		PrintOptions options = parseOptions(request);
		print(request, options);
	}

	private PrintOptions parseOptions(HttpServerRequest request) {
		String sView = request.params().get("view");
		String sFormat = request.params().get("format");
		String sDateBegin = request.params().get("dateBegin");
		String sDateEnd = request.params().get("dateEnd");
		String sColor = request.params().get("color");
		String sShowDetail = request.params().get("showDetail");
		String sLayout = request.params().get("layout");
		List<String> calendars = request.params().getAll("calendarUids");
		List<String> calendarsColors = request.params().getAll("calendarColors");

		PrintOptions opts = new PrintOptions();
		if (sView != null) {
			opts.view = PrintView.valueOf(sView);
		}

		if (sFormat != null) {
			opts.format = PrintFormat.valueOf(sFormat);
		}

		if (sDateBegin != null) {
			opts.dateBegin = BmDateTimeWrapper.create(sDateBegin);
		}

		if (sDateEnd != null) {
			opts.dateEnd = BmDateTimeWrapper.create(sDateEnd);
		}

		if (sColor != null) {
			opts.color = Boolean.parseBoolean(sColor);
		}

		if (sShowDetail != null) {
			opts.showDetail = Boolean.parseBoolean(sShowDetail);
		}

		if (sLayout != null) {
			opts.layout = PrintLayout.valueOf(sLayout);
		}

		if (calendars != null) {
			JsonArray values = new JsonArray(calendars);
			ArrayList<CalendarMetadata> cals = new ArrayList<CalendarMetadata>(values.size());
			for (int i = 0; i < calendars.size(); i++) {
				String uid = calendars.get(i);
				String color = calendarsColors.get(i);
				cals.add(CalendarMetadata.create(uid, color));
			}
			opts.calendars = cals;
		}
		return opts;
	}

	protected void print(final HttpServerRequest request, PrintOptions options) {
		VertxServiceProvider provider = getProvider(request);
		IPrintAsync service = provider.instance("bm/core", IPrintAsync.class);
		service.print(options, new AsyncHandler<PrintData>() {

			@Override
			public void success(PrintData value) {
				HttpServerResponse resp = request.response();
				resp.headers().add("Content-Disposition", "attachment; filename=\"calendar.pdf\"");
				resp.headers().add("Content-Type", "application/pdf");
				resp.end(Buffer.buffer(java.util.Base64.getDecoder().decode(value.data.getBytes())));
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
