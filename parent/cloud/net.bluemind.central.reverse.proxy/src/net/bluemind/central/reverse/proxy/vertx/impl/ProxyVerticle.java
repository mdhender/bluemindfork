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
package net.bluemind.central.reverse.proxy.vertx.impl;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.net.OpenSSLEngineOptions;
import net.bluemind.central.reverse.proxy.model.ProxyInfoStoreClient;
import net.bluemind.central.reverse.proxy.vertx.AuthMatcher;
import net.bluemind.central.reverse.proxy.vertx.HttpProxy;
import net.bluemind.central.reverse.proxy.vertx.HttpServerRequestContext;
import net.bluemind.central.reverse.proxy.vertx.WebSocketProxy;

public class ProxyVerticle extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(ProxyVerticle.class);

	@Override
	public void start(Promise<Void> p) {
		HttpClient proxyClient = vertx.createHttpClient(new HttpClientOptions() //
				.setKeepAlive(true).setTcpKeepAlive(true).setTcpNoDelay(true).setMaxPoolSize(200) //
				.setMaxWebSockets(200) //
				.setSslEngineOptions(new OpenSSLEngineOptions())//
				// le certificat ssl n'est pas valide pour l'ip
				.setSsl(true).setVerifyHost(false));

		logger.info("Client created {}", proxyClient);

		HttpProxy httpProxy = HttpProxy.reverseProxy(proxyClient);
		AuthMatcher<HttpServerRequestContext> requestInfoMatcher = AuthMatcher.requestMatcher();
		ProxyInfoStoreClient storeClient = ProxyInfoStoreClient.create(vertx);
		httpProxy.originSelector(new DownstreamSelector<>(requestInfoMatcher, storeClient));
		httpProxy.responseHook(CompositeResponseHook
				.of(Arrays.asList(new LoginCookieHook(requestInfoMatcher), new ProxyLogHook(requestInfoMatcher))));

		WebSocketProxy webSocketProxy = WebSocketProxy.reverseProxy(proxyClient);
		AuthMatcher<ServerWebSocket> webSocketInfoMatcher = AuthMatcher.webSocketMatcher();
		webSocketProxy.originSelector(new DownstreamSelector<>(webSocketInfoMatcher, storeClient));

		HttpServer proxyServer = vertx.createHttpServer();
		proxyServer.requestHandler(httpProxy).webSocketHandler(webSocketProxy).listen(8080);

		p.complete();
	}
}
