
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

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.parsetools.RecordParser;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.SharedMap;
import net.bluemind.proxy.support.AHCWithProxy;
import net.bluemind.webmodule.server.NeedVertx;

public class IcsUrlCheckHandler implements Handler<HttpServerRequest>, NeedVertx {
	private static final Logger logger = LoggerFactory.getLogger(IcsUrlCheckHandler.class);

	private final AtomicReference<SharedMap<String, String>> sysconf = new AtomicReference<>();

	private final Builder ahcDefaultConfig = AHCWithProxy.defaultConfig()
			.setRequestTimeout((int) TimeUnit.MILLISECONDS.convert(3, TimeUnit.SECONDS))
			.setPooledConnectionIdleTimeout((int) TimeUnit.MILLISECONDS.convert(2, TimeUnit.SECONDS));

	private Vertx vertx;

	private class CalendarNameHandler extends AsyncCompletionHandlerBase {
		private final CompletableFuture<String> calName = new CompletableFuture<>();

		public CalendarNameHandler(Promise<String> calNamePromise, HttpServerRequest request, String url) {
			calName.whenComplete((calName, e) -> complete(calNamePromise, calName, e));
		}

		private void complete(Promise<String> calNamePromise, String calName, Throwable e) {
			if (e != null) {
				calNamePromise.fail(e);
				return;
			}

			calNamePromise.complete(calName);
		}

		@Override
		public State onStatusReceived(HttpResponseStatus status) throws Exception {
			if (status.getStatusCode() >= 400) {
				calName.completeExceptionally(
						new Exception(String.format("%s (%d)", status.getStatusText(), status.getStatusCode())));
				return State.ABORT;
			}

			return super.onStatusReceived(status);
		}

		RecordParser lineSplit = RecordParser.newDelimited("\n", event -> {
			String line = event.toString();
			if (line.startsWith("X-WR-CALNAME")) {
				calName.complete(line.substring(line.indexOf(':') + 1).replace("\r", "").trim());
			}
		});

		@Override
		public State onBodyPartReceived(HttpResponseBodyPart content) throws Exception {
			lineSplit.handle(Buffer.buffer(content.getBodyPartBytes()));
			return calName.isDone() ? State.ABORT : State.CONTINUE;
		}

		@Override
		public void onThrowable(Throwable t) {
			calName.completeExceptionally(t);
		}

		@Override
		public Response onCompleted(Response response) throws Exception {
			if (!calName.isDone()) {
				calName.complete(null);
			}

			return response;
		}
	}

	public IcsUrlCheckHandler() {
		MQ.init().thenAccept(v -> sysconf.set(MQ.sharedMap("system.configuration")));
	}

	@Override
	public void handle(final HttpServerRequest request) {
		String url = request.params().get("url");

		try {
			Uri.create(url);
		} catch (Throwable mue) {
			errorHandler(request, url, mue);
			return;
		}

		vertx.executeBlocking((Promise<String> calNamePromise) -> AHCWithProxy
				.build(ahcDefaultConfig,
						Optional.ofNullable(sysconf.get()).map(SharedMap::asMap).orElse(Collections.emptyMap()))
				.prepareGet(url)
				.addHeader("User-Agent",
						"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_2) AppleWebKit/604.4.7 (KHTML, like Gecko) Version/11.0.2 Safari/604.4.7")
				.execute(new CalendarNameHandler(calNamePromise, request, url)), false,
				calNameAsync -> completeHandler(request, url, calNameAsync));

	}

	private void completeHandler(HttpServerRequest request, String url, AsyncResult<String> calNameAsync) {
		if (calNameAsync.failed()) {
			errorHandler(request, url, calNameAsync.cause());
			return;
		}

		logger.info("Found calendar name '{}' at {}", calNameAsync.result(), url);
		request.response().headers().add("X-Location", url);
		request.response().setStatusCode(200);

		if (calNameAsync.result() != null) {
			request.response().end(calNameAsync.result());
			return;
		}

		request.response().end();
	}

	private void errorHandler(HttpServerRequest request, String url, Throwable e) {
		if (logger.isDebugEnabled()) {
			logger.debug("Error during URL checking: {}", url, e);
		} else {
			logger.error("Error during URL checking: {}: {}", url, e.getMessage());
		}

		HttpServerResponse resp = request.response();
		resp.setStatusCode(500);
		resp.setStatusMessage(e.getMessage() != null ? e.getMessage() : "null");
		resp.end();
	}

	@Override
	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}
}
