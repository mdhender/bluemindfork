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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

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

	private static Cache<String, Semaphore> lockBySession = CacheBuilder.newBuilder()
			.expireAfterAccess(1, TimeUnit.MINUTES).build();

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

	@Override
	public void handle(final HttpServerRequest request) {
		String sessionId = request.headers().get("BMSessionId");

		Semaphore sem;
		try {
			sem = lockBySession.get(sessionId, () -> {
				return new Semaphore(1);
			});
		} catch (ExecutionException e1) {
			request.response().setStatusCode(500);
			request.response().setStatusMessage("Failed to get semaphore for session " + sessionId);
			request.response().end();
			return;
		}

		vertx.executeBlocking(v -> {
			try {
				sem.acquire();
				v.complete();
			} catch (InterruptedException e) {
				v.fail("sem.acquire failed, cant response to request.. " + e);
			}
		}, res -> {
			if (res.failed()) {
				request.response().setStatusCode(500);
				request.response().setStatusMessage(res.cause().toString());
				request.response().end();
				sem.release();
			} else {
				fetchFromCore(request, sessionId).thenAccept(v -> {
					sem.release();
				}).exceptionally(e -> {
					request.response().setStatusCode(500);
					request.response().setStatusMessage(e.getMessage());
					request.response().end();
					sem.release();
					return null;
				});
			}
		});

	}

	private CompletableFuture<Void> fetchFromCore(final HttpServerRequest request, String sessionId) {
		final VertxPromiseServiceProvider clientProvider = new VertxPromiseServiceProvider(prov, locator, sessionId);

		String folderUid = request.params().get("folderUid");
		IMailboxItemsPromise service = clientProvider.instance(IMailboxItemsPromise.class, folderUid);

		String imapUid = request.params().get("imapUid");
		String address = request.params().get("address");
		String encoding = request.params().get("encoding");
		String mime = request.params().get("mime");
		String charset = request.params().get("charset");
		String filename = request.params().get("filename");

		return service.fetch(Long.parseLong(imapUid), address, encoding, mime, charset, filename)
				.thenAccept(partContent -> {
					HttpServerResponse resp = request.response();
					resp.setChunked(true);

					resp.headers().set("Content-Type", mime + ";charset=" + charset);
					resp.headers().set("Content-Disposition", "inline; filename=\"" + filename + "\"");
					resp.headers().set("Cache-Control", "max-age=15768000, private"); // 6 months

					ReadStream<Buffer> read = VertxStream.read(partContent);
					read.pipeTo(resp);
				});
	}

}
