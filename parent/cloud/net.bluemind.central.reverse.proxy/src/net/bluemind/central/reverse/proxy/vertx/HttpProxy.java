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

import java.util.function.BiConsumer;
import java.util.function.Function;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import net.bluemind.central.reverse.proxy.vertx.impl.CloseableSession;
import net.bluemind.central.reverse.proxy.vertx.impl.HttpProxyImpl;

/**
 * Handles the HTTP reverse proxy logic between the <i><b>user agent</b></i> and
 * the <i><b>origin</b></i>.
 * <p>
 * 
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface HttpProxy extends Handler<HttpServerRequest> {

	/**
	 * Create a new {@code HttpProxy} instance.
	 *
	 * @param client the {@code HttpClient} that forwards <i><b>outbound</b></i>
	 *               requests to the <i><b>origin</b></i>.
	 * @return a reference to this, so the API can be used fluently.
	 */
	static HttpProxy reverseProxy(String deploymentID, HttpClient client) {
		return new HttpProxyImpl(deploymentID, new ProxyOptions(), client);
	}

	/**
	 * Create a new {@code HttpProxy} instance.
	 *
	 * @param client the {@code HttpClient} that forwards <i><b>outbound</b></i>
	 *               requests to the <i><b>origin</b></i>.
	 * @return a reference to this, so the API can be used fluently.
	 */
	static HttpProxy reverseProxy(String deploymentID, ProxyOptions options, HttpClient client) {
		return new HttpProxyImpl(deploymentID, options, client);
	}

	/**
	 * Set the {@code SocketAddress} of the <i><b>origin</b></i>.
	 *
	 * @param address the {@code SocketAddress} of the <i><b>origin</b></i>
	 * @return a reference to this, so the API can be used fluently
	 */
	default HttpProxy origin(SocketAddress address) {
		return originSelector(req -> Future.succeededFuture(new CloseableSession(address)));
	}

	/**
	 * Set the host name and port number of the <i><b>origin</b></i>.
	 *
	 * @param port the port number of the <i><b>origin</b></i> server
	 * @param host the host name of the <i><b>origin</b></i> server
	 * @return a reference to this, so the API can be used fluently
	 */
	default HttpProxy origin(int port, String host) {
		return origin(SocketAddress.inetSocketAddress(port, host));
	}

	/**
	 * Set a selector that resolves the <i><b>origin</b></i> address based on the
	 * <i><b>outbound</b></i> request.
	 *
	 * @param selector the selector
	 * @return a reference to this, so the API can be used fluently
	 */
	HttpProxy originSelector(Function<HttpServerRequestContext, Future<CloseableSession>> selector);

	HttpProxy responseHook(BiConsumer<HttpServerRequestContext, ProxyResponse> responseHook);

	/**
	 * Handle the <i><b>outbound</b></i> {@code HttpServerRequest}.
	 *
	 * @param outboundRequest the outbound {@code HttpServerRequest}
	 */
	void handle(HttpServerRequest outboundRequest);

}
