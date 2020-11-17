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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.streams.Pump;
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

	private static Logger logger = LoggerFactory.getLogger(PartContentUrlHandler.class);
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
		String sessionId = request.headers().get("BMSessionId");
		final VertxPromiseServiceProvider clientProvider = new VertxPromiseServiceProvider(prov, locator, sessionId);

		String folderUid = request.params().get("folderUid");
		IMailboxItemsPromise service = clientProvider.instance(IMailboxItemsPromise.class, folderUid);

		String imapUid = request.params().get("imapUid");
		String address = request.params().get("address");
		String encoding = request.params().get("encoding");
		String mime = request.params().get("mime");
		String charset = request.params().get("charset");
		String filename = request.params().get("filename");

		service.fetch(Long.parseLong(imapUid), address, encoding, mime, charset, filename).thenAccept(partContent -> {
			HttpServerResponse resp = request.response();
			resp.setChunked(true);

			resp.headers().set("Content-Type", mime + ";charset=" + charset);
			resp.headers().set("Content-Disposition", "attachment; filename=\"" + filename + "\"");
			resp.headers().set("Cache-Control", "max-age=15768000, private"); // 6 months

			ReadStream<Buffer> read = VertxStream.read(partContent);
			Pump pump = Pump.pump(read, resp);
			pump.start();
			read.endHandler(v -> resp.end());
		}).exceptionally(e -> {
			logger.error("error when fetching part, {}, {}, {}, {}, {}, {}, {}, {}", folderUid, imapUid, address,
					encoding, mime, charset, filename, e);
			request.response().setStatusCode(500);
			request.response().setStatusMessage(e.getMessage());
			request.response().end();
			return null;
		});

	}

}
