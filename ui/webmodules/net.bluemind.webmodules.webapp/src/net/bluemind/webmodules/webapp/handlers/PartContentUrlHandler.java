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
package net.bluemind.webmodules.webapp.handlers;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.streams.ReadStream;
import net.bluemind.backend.mail.api.IMailboxItemsPromise;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.ILocator;
import net.bluemind.core.rest.http.VertxPromiseServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.network.topology.Topology;
import net.bluemind.webmodule.server.NeedVertx;

public class PartContentUrlHandler implements Handler<HttpServerRequest>, NeedVertx {

	private static Cache<String, Semaphore> lockBySession = Caffeine.newBuilder()
			.expireAfterAccess(20, TimeUnit.MINUTES).build();

	private static final Logger logger = LoggerFactory.getLogger(PartContentUrlHandler.class);

	private HttpClientProvider prov;
	private Vertx vertx;

	@Override
	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
		this.prov = new HttpClientProvider(vertx);
	}

	private static final ILocator locator = (String service, AsyncHandler<String[]> asyncHandler) -> {
		String core = Topology.get().core().value.address();
		String[] resp = new String[] { core };
		asyncHandler.success(resp);
	};

	@FunctionalInterface
	public interface SerializedOperation {

		CompletableFuture<Void> runWithLock(HttpServerRequest req, String coreSid);

	}

	@Override
	public void handle(final HttpServerRequest request) {
		String sessionId = request.headers().get("BMSessionId");

		request.pause();

		withSessionLock(sessionId, request, this::fetchFromCore);
	}

	private void withSessionLock(String sessionId, HttpServerRequest request, SerializedOperation operation) {
		Semaphore sem = lockBySession.get(sessionId, sid -> new Semaphore(1, true));

		try {
			boolean locked = sem.tryAcquire(25, TimeUnit.MILLISECONDS);
			if (!locked) {
				vertx.setTimer(5, tid -> withSessionLock(sessionId, request, operation));
			} else {
				request.resume();
				operation.runWithLock(request, sessionId).whenComplete((v, e) -> {
					sem.release();
					if (e != null) {
						logger.error("[{}] {}", sessionId, e.getMessage(), e);
						HttpServerResponse resp = request.response();
						if (!resp.headWritten()) {
							request.response().setStatusCode(500).setStatusMessage(e.getMessage());
						}
						if (!resp.ended()) {
							resp.end();
						}
					}
				});
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private CompletableFuture<Void> fetchFromCore(final HttpServerRequest request, String sessionId) {
		VertxPromiseServiceProvider clientProvider = new VertxPromiseServiceProvider(prov, locator, sessionId);

		String folderUid = request.params().get("folderUid");
		IMailboxItemsPromise service = clientProvider.instance(IMailboxItemsPromise.class, folderUid);

		String imapUid = request.params().get("imapUid");
		String address = request.params().get("address");
		String encoding = request.params().get("encoding");
		String mime = request.params().get("mime");
		String charset = request.params().get("charset");
		String filename = request.params().get("filename");

		return service.fetch(Long.parseLong(imapUid), address, encoding, mime, charset, filename)
				.thenCompose(partContent -> {
					HttpServerResponse resp = request.response();
					resp.setChunked(true);

					resp.headers().set("Content-Type", mime + ";charset=" + charset);
					resp.headers().set("Content-Disposition", "inline; filename=\"" + filename + "\"");
					resp.headers().set("Cache-Control", "max-age=15768000, private"); // 6 months

					ReadStream<Buffer> read = VertxStream.read(partContent);
					return read.pipeTo(resp).toCompletionStage();
				});
	}

}
