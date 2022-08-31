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
package net.bluemind.webmodule.filehostinghandler;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.streams.ReadStream;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.Stream;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.ILocator;
import net.bluemind.core.rest.http.ITaggedServiceProvider;
import net.bluemind.core.rest.http.VertxServiceProvider;
import net.bluemind.filehosting.api.FileHostingItem;
import net.bluemind.filehosting.api.IFileHostingAsync;
import net.bluemind.filehosting.api.Metadata;
import net.bluemind.network.topology.Topology;
import net.bluemind.webmodule.server.IWebFilter;
import net.bluemind.webmodule.server.NeedVertx;

public class FileHostingHandler implements IWebFilter, NeedVertx {
	Logger logger = LoggerFactory.getLogger(FileHostingHandler.class);
	private HttpClientProvider clientProvider;

	@Override
	public CompletableFuture<HttpServerRequest> filter(HttpServerRequest request) {
		final HttpServerResponse resp = request.response();

		String path = request.path();
		if (path.contains("fh/bm-fh")) {
			final String uid = extractUid(request.absoluteURI());
			logger.info("Handling request to shared file uid: {}", uid);

			getService(request).getComplete(uid, new AsyncHandler<FileHostingItem>() {

				@Override
				public void success(FileHostingItem item) {
					loadSharedFile(request, resp, uid, item);
				}

				@Override
				public void failure(Throwable e) {
					errorHandling(resp, uid, e);

				}
			});

			return CompletableFuture.completedFuture(null);
		}

		return CompletableFuture.completedFuture(request);
	}

	private void loadSharedFile(final HttpServerRequest request, final HttpServerResponse resp, final String uid,
			final FileHostingItem item) {
		logger.info("Delivering shared file: {}", item.name);
		getMetaData(item.metadata, "content-length").map(ct -> {
			logger.debug("Setting content-length to '{}'", ct);
			return resp.putHeader("Content-Length", ct);
		}).orElseGet(() -> { // NOSONAR
			logger.debug("Chunked response for {}", item.name);
			return resp.setChunked(true);
		});

		resp.putHeader("Access-Control-Allow-Origin", "*");
		resp.putHeader("Content-Disposition", String.format("inline; filename=\"%s\";", item.name));
		getMetaData(item.metadata, "mime-type").ifPresent(ct -> resp.putHeader("Content-Type", ct));
		getService(request).getSharedFile(uid, new AsyncHandler<Stream>() {

			@Override
			public void success(Stream stream) {
				streamFile(resp, stream);
			}

			@Override
			public void failure(Throwable e) {
				errorHandling(resp, uid, e);
			}

		});
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void streamFile(final HttpServerResponse resp, final Stream stream) {
		resp.setStatusCode(200);
		ReadStream<Buffer> readStream = (ReadStream) stream;
		readStream.pipeTo(resp, ar -> {
			if (ar.failed()) {
				logger.error("FH proxy {}", ar.cause().getMessage(), ar.cause());
			}
		});
		readStream.resume();
	}

	private void errorHandling(HttpServerResponse resp, String uid, Throwable e) {
		String msg = String.format("Shared file %s not found", uid);
		logger.warn(msg);
		printStackTrace(e);
		resp.setStatusCode(404).setStatusMessage(msg).end();
	}

	private void printStackTrace(Throwable e) {
		if (logger.isDebugEnabled()) {
			StackTraceElement[] st = e.getStackTrace();
			for (StackTraceElement element : st) {
				logger.debug(element.toString());
			}
		}
	}

	private String extractUid(String absoluteURI) {
		int idx = absoluteURI.indexOf("fh/bm-fh/");
		if (idx > 0) {
			return absoluteURI.substring(idx + "fh/bm-fh/".length());
		} else {
			throw new IllegalStateException(absoluteURI + " does not match");
		}
	}

	protected Optional<String> getMetaData(List<Metadata> metadata, String key) {
		for (Metadata meta : metadata) {
			if (meta.key.equals(key)) {
				return Optional.ofNullable(meta.value);
			}
		}
		return Optional.empty();
	}

	protected IFileHostingAsync getService(HttpServerRequest request) {
		ITaggedServiceProvider sp = getProvider(null, request);
		return sp.instance("bm/core", IFileHostingAsync.class, "default");
	}

	private static final ILocator locator = (String service, AsyncHandler<String[]> asyncHandler) -> {
		String core = Topology.get().core().value.address();
		String[] resp = new String[] { core };
		asyncHandler.success(resp);
	};

	private ITaggedServiceProvider getProvider(String apiKey, HttpServerRequest request) {
		return new VertxServiceProvider(clientProvider, locator, apiKey).from(request);
	}

	@Override
	public void setVertx(Vertx vertx) {
		clientProvider = new HttpClientProvider(vertx);
	}

}
