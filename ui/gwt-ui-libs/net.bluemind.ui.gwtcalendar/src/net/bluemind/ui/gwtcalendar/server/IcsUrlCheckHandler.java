
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

import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.CharStreams;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.utils.Trust;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarParser;
import net.fortuna.ical4j.data.CalendarParserFactory;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Property;

public class IcsUrlCheckHandler implements Handler<HttpServerRequest> {
	private static final Logger logger = LoggerFactory.getLogger(IcsUrlCheckHandler.class);

	@Override
	public void handle(final HttpServerRequest request) {
		String url = request.params().get("url");
		try {
			connect(request, url);
		} catch (Exception e) {
			logger.info("ICS url not valid", e);
			request.response().setStatusCode(500);
			request.response().end();
		}
	}

	private void connect(final HttpServerRequest request, final String url) throws Exception {
		URL urlp = new URL(url);
		HttpURLConnection conn = (HttpURLConnection) urlp.openConnection();
		if (conn instanceof HttpsURLConnection) {
			((HttpsURLConnection) conn).setHostnameVerifier(Trust.acceptAllVerifier());
			SSLContext sc = Trust.createSSLContext();
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			((HttpsURLConnection) conn).setSSLSocketFactory(sc.getSocketFactory());
		}

		String content = CharStreams.toString(new InputStreamReader(conn.getInputStream()));
		int status = conn.getResponseCode();
		if (status == 301) {
			String location = conn.getHeaderField("Location");
			logger.debug("ICS url {} has moved permanently to {}", url, location);
			connect(request, location);
		} else {
			if (status > 301) {
				logger.debug("Cannot verify ICS url {}:{}", urlp.toString(), status);
				request.response().setStatusCode(500);
				request.response().end();
			} else {
				parseResponse(request, content.toString(), url);
			}
		}
	}

	private void parseResponse(HttpServerRequest request, String content, String url) throws Exception {
		CalendarParser parser = CalendarParserFactory.getInstance().createParser();
		CalendarBuilder builder = new CalendarBuilder(parser);
		request.response().headers().add("X-Location", url);
		Calendar cal = builder.build(new StringReader(content));
		Property p = cal.getProperty("X-WR-CALNAME");
		if (p != null) {
			request.response().setStatusCode(200);
			request.response().end(p.getValue());
		} else {
			request.response().setStatusCode(200);
			request.response().end();
		}
	}

}
