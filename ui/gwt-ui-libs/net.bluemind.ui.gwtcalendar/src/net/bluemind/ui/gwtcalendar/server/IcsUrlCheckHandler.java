
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

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.asynchttpclient.AsyncCompletionHandlerBase;
import org.asynchttpclient.DefaultAsyncHttpClientConfig.Builder;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseStatus;
import org.asynchttpclient.Response;
import org.asynchttpclient.uri.Uri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.parsetools.RecordParser;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.SharedMap;
import net.bluemind.proxy.support.AHCWithProxy;

public class IcsUrlCheckHandler implements Handler<HttpServerRequest> {
	private static final Logger logger = LoggerFactory.getLogger(IcsUrlCheckHandler.class);

	private final AtomicReference<SharedMap<String, String>> sysconf = new AtomicReference<>();

	private final Builder ahcDefaultConfig = AHCWithProxy.defaultConfig()
			.setRequestTimeout((int) TimeUnit.MILLISECONDS.convert(3, TimeUnit.SECONDS))
			.setPooledConnectionIdleTimeout((int) TimeUnit.MILLISECONDS.convert(2, TimeUnit.SECONDS));

	public IcsUrlCheckHandler() {
		MQ.init().thenAccept(v -> sysconf.set(MQ.sharedMap("system.configuration")));
	}

	@Override
	public void handle(final HttpServerRequest request) {
		String url = request.params().get("url");

		try {
			Uri.create(url);
		} catch (Throwable mue) {
			errorHandler(request, url).handle(mue);
			return;
		}

		connect(request, url);
	}

	private void connect(final HttpServerRequest request, final String url) {
		AHCWithProxy
				.build(ahcDefaultConfig,
						Optional.ofNullable(sysconf.get()).map(SharedMap::asMap).orElse(Collections.emptyMap()))
				.prepareGet(url).addHeader("content-type", "application/json;charset=UTF-8")
				.execute(new AsyncCompletionHandlerBase() {
					private CompletableFuture<String> calName = new CompletableFuture<String>();

					@Override
					public State onStatusReceived(HttpResponseStatus status) throws Exception {
						if (status.getStatusCode() >= 400) {
							errorHandler(request, url)
									.handle(new Exception(String.format("Unable to get ICS from %s: %s (%d)", url,
											status.getStatusText(), status.getStatusCode())));
							return State.ABORT;
						}

						return super.onStatusReceived(status);
					}

					RecordParser lineSplit = RecordParser.newDelimited("\n", event -> {
						String line = event.toString();
						if (line.startsWith("X-WR-CALNAME")) {
							completeHandler(request, url)
									.handle(line.substring(line.indexOf(':') + 1).replace("\r", "").trim());
							calName.complete(null);
						}
					});

					@Override
					public State onBodyPartReceived(HttpResponseBodyPart content) throws Exception {
						lineSplit.handle(Buffer.buffer(content.getBodyPartBytes()));
						return calName.isDone() ? State.ABORT : State.CONTINUE;
					}

					@Override
					public void onThrowable(Throwable t) {
						errorHandler(request, url).handle(t);
					}

					@Override
					public Response onCompleted(Response response) throws Exception {
						if (!calName.isDone()) {
							completeHandler(request, url).handle(null);
						}

						return response;
					}
				});
	}

	private Handler<String> completeHandler(HttpServerRequest request, String url) {
		return calName -> {
			logger.info("Found calendar name '{}' at {}", calName, url);
			request.response().headers().add("X-Location", url);
			request.response().setStatusCode(200);

			if (calName == null) {
				request.response().end();
			} else {
				request.response().end(calName);
			}
		};
	}

	private Handler<Throwable> errorHandler(HttpServerRequest request, String url) {
		return e -> {
			if (logger.isDebugEnabled()) {
				logger.debug("Error during URL checking: {}", url, e);
			} else {
				logger.error("Error during URL checking: {}", url);
			}

			HttpServerResponse resp = request.response();
			resp.setStatusCode(500);
			resp.setStatusMessage(e.getMessage() != null ? e.getMessage() : "null");
			resp.end();
		};
	}
}
