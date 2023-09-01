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
package net.bluemind.core.rest.http.vertx;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.base.IRestCallHandler;
import net.bluemind.core.rest.base.RestRequest;
import net.bluemind.core.rest.base.RestResponse;
import net.bluemind.core.rest.log.CallLogger;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.lib.vertx.utils.MmapWriteStream;

public class RestHttpProxyHandler implements Handler<HttpServerRequest> {
	static final Logger logger = LoggerFactory.getLogger(RestHttpProxyHandler.class);
	private static final long MAX_CONTENT_LENGTH = 1000 * 1000 * 10; // 10m
	private Vertx vertx;
	private IRestCallHandler proxy;

	private static final CharSequence HEADER_PRAGMA = HttpHeaders.createOptimized("Pragma");
	private static final CharSequence HEADER_PRAGMA_VALUE = HttpHeaders.createOptimized("no-cache");
	private static final List<CharSequence> HEADER_CACHE_CONTROL_VALUE = Arrays.asList("no-cache", "no-store",
			"must-revalidate");
	private static final String CALL_CLASS = RestHttpProxyHandler.class.getSimpleName();

	public RestHttpProxyHandler(Vertx vertx, IRestCallHandler proxy) {
		this.vertx = vertx;
		logger.debug("fix warning {}", this.vertx);
		this.proxy = proxy;
	}

	@Override
	public void handle(final HttpServerRequest request) {
		request.exceptionHandler(exceptionHandler(request));
		String te = request.headers().get(HttpHeaders.TRANSFER_ENCODING);
		boolean chunked = "chunked".equals(te);
		logger.debug("chunked {} : {} : {}:{}", chunked, te, request.path(), request.method());

		String remoteAddress = request.remoteAddress().host();
		final RestRequest rr = RestRequest.create(remoteAddress, request.method(), request.headers(), request.path(),
				request.params(), null, null);

		AsyncHandler<RestResponse> handler = responseHandler(request);
		AsyncHandler<RestResponse> wrapped = CallLogger.start(CALL_CLASS, rr).responseHandler(handler);

		request.exceptionHandler(wrapped::failure);

		if (chunked) {
			rr.bodyStream = request;
			handleBody(request, rr, wrapped);
		} else {
			String clAsString = request.headers().get(HttpHeaders.CONTENT_LENGTH);
			Long contentLength = null;
			try {
				contentLength = Long.parseLong(clAsString);
			} catch (NumberFormatException e) {
			}
			if (contentLength != null && contentLength > MAX_CONTENT_LENGTH) {
				rr.maxContentLengthExceeded = true;
				setMappedBody(request, rr, wrapped, contentLength);
			} else {
				request.bodyHandler(body -> {
					rr.body = body;
					handleBody(request, rr, wrapped);
				});
			}
		}
	}

	private void setMappedBody(HttpServerRequest request, RestRequest rr, AsyncHandler<RestResponse> wrapped,
			Long contentLength) {
		try {
			File tmp = new File(System.getProperty("java.io.tmpdir"));
			MmapWriteStream out = new MmapWriteStream(tmp.toPath(), contentLength);
			request.pipeTo(out, handler -> {
				try {
					rr.body = Buffer.buffer(out.mmap().get());
					handleBody(request, rr, wrapped);
				} catch (Exception e) {
					wrapped.failure(e);
				}
			});
			request.resume();
		} catch (Exception e) {
			wrapped.failure(e);
		}
	}

	protected AsyncHandler<RestResponse> responseHandler(final HttpServerRequest request) {
		return new AsyncHandler<RestResponse>() {

			@Override
			public void success(RestResponse value) {
				request.response().setStatusCode(value.statusCode);
				MultiMap headers = request.response().headers();
				headers.addAll(value.headers);

				headers.add(HttpHeaders.CACHE_CONTROL, HEADER_CACHE_CONTROL_VALUE);
				headers.add(HEADER_PRAGMA, HEADER_PRAGMA_VALUE);
				if (value.responseStream != null) {
					request.response().setChunked(true);
					value.responseStream.pipeTo(request.response(), ar -> {
						if (ar.succeeded()) {
							logger.debug("response pipe finished success");
						} else {
							logger.error("response pipe finished error: {}", ar.cause().getMessage(), ar.cause());
						}
					});
					value.responseStream.resume();
				} else {
					logger.debug("send response {}", value);
					if (value.data != null) {
						logger.debug("send end {}byte(s)", value.data.length());
						request.response().end(value.data);
					} else {
						logger.debug("send end");
						request.response().end();
					}
				}
			}

			@Override
			public void failure(Throwable e) {
				logger.debug("send error", e);
				request.response().setStatusCode(500);
				request.response().end(JsonUtils.asString(new ServerFault(e.getMessage(), ErrorCode.UNKNOWN)));
			}
		};
	}

	protected void handleBody(final HttpServerRequest request, RestRequest rr,
			AsyncHandler<RestResponse> asyncHandler) {
		if (rr.bodyStream != null) {
			rr.bodyStream.pause();
		}

		proxy.call(rr, asyncHandler);
	}

	// handlers proxies
	protected void handleExceptionDuringRequest(HttpServerRequest request, Throwable throwable) {
		logger.error("not handled exception during request", throwable);
		if (throwable instanceof IndexOutOfBoundsException) {
			request.response().setStatusCode(413);
		} else if (throwable instanceof IllegalArgumentException) {
			request.response().setStatusCode(411);
		} else {
			request.response().setStatusCode(500);
		}
		request.response().setStatusMessage(throwable.getMessage() != null ? throwable.getMessage() : "null");
		request.response().end();
	}

	private Handler<Throwable> exceptionHandler(final HttpServerRequest request) {
		return throwable -> handleExceptionDuringRequest(request, throwable);
	}

}
