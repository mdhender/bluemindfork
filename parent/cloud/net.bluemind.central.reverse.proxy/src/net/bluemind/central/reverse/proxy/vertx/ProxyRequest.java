/*
 * Copyright (c) 2011-2020 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package net.bluemind.central.reverse.proxy.vertx;

import java.util.function.Function;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.streams.ReadStream;
import net.bluemind.central.reverse.proxy.vertx.impl.ProxyRequestImpl;

/**
 *
 * Handles the interoperability of the <b>request</b> between the <i><b>user
 * agent</b></i> and the <i><b>origin</b></i>.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface ProxyRequest {

	/**
	 * Create a new {@code ProxyRequest} instance, the outbound request will be
	 * paused.
	 *
	 * @param outboundRequest the {@code HttpServerRequest} of the <i><b>user
	 *                        agent</b></i>
	 * @return a reference to this, so the API can be used fluently
	 */
	static ProxyRequest reverseProxy(HttpServerRequest outboundRequest) {
		outboundRequest.pause();
		return new ProxyRequestImpl(outboundRequest);
	}

	/**
	 * @return the HTTP version of the outbound request
	 */
	HttpVersion version();

	/**
	 * @return the absolute URI of the outbound request
	 */
	String absoluteURI();

	/**
	 * @return the HTTP method to be sent to the <i><b>origin</b></i> server.
	 */
	HttpMethod getMethod();

	/**
	 * Set the HTTP method to be sent to the <i><b>origin</b></i> server.
	 *
	 * <p>
	 * The initial HTTP method value is the outbound request HTTP method.
	 *
	 * @param method the new HTTP method
	 * @return a reference to this, so the API can be used fluently
	 */
	ProxyRequest setMethod(HttpMethod method);

	/**
	 * @return the request URI to be sent to the <i><b>origin</b></i> server.
	 */
	String getURI();

	/**
	 * Set the request URI to be sent to the <i><b>origin</b></i> server.
	 *
	 * <p>
	 * The initial request URI value is the <i><b>outbound</b></i> request URI.
	 *
	 * @param uri the new URI
	 * @return a reference to this, so the API can be used fluently
	 */
	ProxyRequest setURI(String uri);

	/**
	 * @return the request body to be sent to the <i><b>origin</b></i> server.
	 */
	Body getBody();

	/**
	 * Set the request body to be sent to the <i><b>origin</b></i> server.
	 *
	 * <p>
	 * The initial request body value is the <i><b>outbound</b></i> request body.
	 *
	 * @param body the new body
	 * @return a reference to this, so the API can be used fluently
	 */
	ProxyRequest setBody(Body body);

	/**
	 * @return the headers that will be sent to the origin server, the returned
	 *         headers can be modified. The headers map is populated with the
	 *         outbound request headers
	 */
	MultiMap headers();

	/**
	 * Put an HTTP header
	 *
	 * @param name  The header name
	 * @param value The header value
	 * @return a reference to this, so the API can be used fluently
	 */
	ProxyRequest putHeader(CharSequence name, CharSequence value);

	/**
	 * Set a body filter.
	 *
	 * <p>
	 * The body filter can rewrite the request body sent to the <i><b>origin</b></i>
	 * server.
	 *
	 * @param filter the filter
	 * @return a reference to this, so the API can be used fluently
	 */
	ProxyRequest bodyFilter(Function<ReadStream<Buffer>, ReadStream<Buffer>> filter);

	/**
	 * Proxy this outbound request and response to the <i><b>origin</b></i> server
	 * using the specified inbound request.
	 *
	 * @param inboundRequest the request connected to the <i><b>origin</b></i>
	 *                       server
	 */
	default Future<Void> proxy(HttpClientRequest inboundRequest) {
		return send(inboundRequest).flatMap(resp -> resp.send());
	}

	/**
	 * Send this request to the <i><b>origin</b></i> server using the specified
	 * inbound request.
	 *
	 * <p>
	 * The {@code completionHandler} will be called with the proxy response sent by
	 * the <i><b>origin</b></i>.
	 *
	 * @param inboundRequest the request connected to the <i><b>origin</b></i>
	 *                       server
	 */
	Future<ProxyResponse> send(HttpClientRequest inboundRequest);

	/**
	 * Release the proxy request.
	 *
	 * <p>
	 * The HTTP server request is resumed, no HTTP server response is sent.
	 *
	 * @return a reference to this, so the API can be used fluently
	 */
	ProxyRequest release();

	/**
	 * @return the outbound HTTP server request
	 */
	HttpServerRequest outboundRequest();

	/**
	 * Create and return the proxy response.
	 *
	 * @return the proxy response
	 */
	ProxyResponse response();

}
