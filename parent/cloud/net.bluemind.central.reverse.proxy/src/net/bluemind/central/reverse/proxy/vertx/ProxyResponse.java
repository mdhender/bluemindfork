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
import io.vertx.core.streams.ReadStream;

/**
 *
 * Handles the interoperability of the <b>response</b> between the
 * <i><b>origin</b></i> and the <i><b>user agent</b></i>.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface ProxyResponse {

	/**
	 *
	 * Return the corresponding {@code ProxyRequest}.
	 *
	 * @return the proxy request
	 */
	ProxyRequest request();

	/**
	 * Get the status code.
	 *
	 * @return the status code to be sent to the <i><b>user agent</b></i>
	 */
	int getStatusCode();

	/**
	 * Set the status code to be sent to the <i><b>user agent</b></i>.
	 *
	 * <p>
	 * The initial value is the inbound response status code.
	 *
	 * @param sc the status code
	 * @return a reference to this, so the API can be used fluently
	 */
	ProxyResponse setStatusCode(int sc);

	/**
	 * Get the status message.
	 *
	 * @return the status message to be sent to the <i><b>user agent</b></i>
	 */
	String getStatusMessage();

	/**
	 * Set the status message to be sent to the <i><b>user agent</b></i>.
	 *
	 * <p>
	 * The initial value is the inbound response status message.
	 *
	 * @param statusMessage the status message
	 * @return a reference to this, so the API can be used fluently
	 */
	ProxyResponse setStatusMessage(String statusMessage);

	/**
	 * @return the headers that will be sent to the <i><b>user agent</b></i>, the
	 *         returned headers can be modified. The headers map is populated with
	 *         the inbound response headers
	 */
	MultiMap headers();

	/**
	 * Put an HTTP header.
	 *
	 * @param name  The header name
	 * @param value The header value
	 * @return a reference to this, so the API can be used fluently
	 */
	ProxyResponse putHeader(CharSequence name, CharSequence value);

	/**
	 * Get the body of the response.
	 *
	 * @return the response body to be sent to the <i><b>user agent</b></i>
	 */
	Body getBody();

	/**
	 * Set the request body to be sent to the <i><b>user agent</b></i>.
	 *
	 * <p>
	 * The initial request body value is the outbound response body.
	 *
	 * @param body the new body
	 * @return a reference to this, so the API can be used fluently
	 */
	ProxyResponse setBody(Body body);

	/**
	 * Set a body filter.
	 *
	 * <p>
	 * The body filter can rewrite the response body sent to the <i><b>user
	 * agent</b></i>.
	 *
	 * @param filter the filter
	 * @return a reference to this, so the API can be used fluently
	 */
	ProxyResponse bodyFilter(Function<ReadStream<Buffer>, ReadStream<Buffer>> filter);

	boolean publicCacheControl();

	long maxAge();

	/**
	 * @return the {@code etag} sent by the <i><b>origin</b></i> response
	 */
	String etag();

	/**
	 * Send the proxy response to the <i><b>user agent</b></i>.
	 */
	Future<Void> send();

	/**
	 * Release the proxy response.
	 *
	 * <p>
	 * The HTTP client inbound response is resumed, no HTTP inbound server response
	 * is sent.
	 */
	ProxyResponse release();

}
