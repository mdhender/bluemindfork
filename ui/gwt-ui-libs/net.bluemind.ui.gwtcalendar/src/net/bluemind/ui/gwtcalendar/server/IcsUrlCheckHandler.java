
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.webmodule.server.NeedVertx;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarParser;
import net.fortuna.ical4j.data.CalendarParserFactory;

public class IcsUrlCheckHandler implements Handler<HttpServerRequest>, NeedVertx {
	private static final Logger logger = LoggerFactory.getLogger(IcsUrlCheckHandler.class);
	private Vertx vertx;

	@Override
	public void handle(final HttpServerRequest request) {
		String url = request.params().get("url");
		connect(request, url);
	}

	@SuppressWarnings("deprecation")
	private void connect(final HttpServerRequest request, final String url) {
		URL urlp;
		try {
			urlp = new URL(url);
		} catch (MalformedURLException e) {

			errorHandler(request, null, null).handle(e);
			return;
		}

		HttpClientOptions opts = new HttpClientOptions().setDefaultHost(urlp.getHost()).setKeepAlive(false)
				.setConnectTimeout(3000);
		if (urlp.getPort() != -1) {
			opts.setDefaultPort(urlp.getPort());
		}
		if (urlp.getProtocol().equals("https")) {
			opts.setSsl(true);
			opts.setTrustAll(true);
			if (urlp.getPort() == -1) {
				opts.setDefaultPort(443);
			}
		}
		String fullpath = urlp.getPath();
		if (urlp.getQuery() != null) {
			fullpath += "?" + urlp.getQuery();
		}

		HttpClient client = vertx.createHttpClient(opts);

		final String fetchUrl = fullpath;
		File tmpFile = new File(System.getProperty("java.io.tmpdir"), System.currentTimeMillis() + ".ics");

		client.getNow(urlp.getPath(), response -> {
			logger.info("handler ret url {}", url);
			if (response.statusCode() == 301) {
				String location = response.headers().get("Location");
				logger.debug("ICS url {} has moved permanently to {}", url, location);
				connect(request, location);
			} else {
				if (response.statusCode() > 301) {
					logger.debug("Cannot verify ICS url {}:{}:{}", fetchUrl, response.statusCode(),
							response.statusMessage());
					request.response().setStatusCode(500);
					request.response().end();
				} else {
					FileWriter out = null;
					try {
						out = new FileWriter(tmpFile);
					} catch (IOException e1) {
					}
					BufferedWriter writer = new BufferedWriter(out);
					request.response().headers().add("X-Location", url);
					request.response().setStatusCode(200);
					response.exceptionHandler(errorHandler(request, writer, tmpFile));
					response.endHandler(h -> {
						try {
							writer.close();
						} catch (IOException e) {
						}
						CompletableFuture<Optional<String>> calName = parseCalName(tmpFile);
						calName.thenAccept(cal -> {
							if (cal.isPresent()) {
								request.response().end(cal.get());
							} else {
								request.response().end();
							}
							tmpFile.delete();
						});
					});
					response.handler(h -> {
						try {
							writer.append(h.toString());
						} catch (IOException e) {

						}
					});
				}
			}
		});
	}

	private CompletableFuture<Optional<String>> parseCalName(File tmpFile) {
		return CompletableFuture.supplyAsync(() -> {
			CalendarParser parser = CalendarParserFactory.getInstance().createParser();
			CalendarBuilder builder = new CalendarBuilder(parser);
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(Files.newInputStream(tmpFile.toPath())))) {
				// X-WR-CALNAME
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.contains("X-WR-CALNAME")) {
						return Optional.of(line.substring(line.indexOf(":") + 1).trim());
					}
				}
			} catch (Exception e) {
				logger.warn("Cannot parse calendar ics", e);
			}
			return Optional.empty();
		});

	}

	private Handler<Throwable> errorHandler(final HttpServerRequest request, BufferedWriter writer, File tmpFile) {
		return e -> {
			logger.error("error during url checking", e);
			HttpServerResponse resp = request.response();
			resp.setStatusCode(500);
			resp.setStatusMessage(e.getMessage() != null ? e.getMessage() : "null");
			resp.end();
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e1) {
				}
			}
			if (tmpFile != null) {
				tmpFile.delete();
			}
		};
	}

	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}

}
