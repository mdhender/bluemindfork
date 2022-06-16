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

import static net.bluemind.central.reverse.proxy.common.ProxyEventBusAddress.ADDRESS;
import static net.bluemind.central.reverse.proxy.common.ProxyEventBusAddress.INSTALLATION_IP_CHANGE_NAME;
import static net.bluemind.central.reverse.proxy.common.ProxyEventBusAddress.MODEL_READY_NAME;
import static net.bluemind.central.reverse.proxy.common.config.CrpConfig.Proxy.KEEP_ALIVE;
import static net.bluemind.central.reverse.proxy.common.config.CrpConfig.Proxy.MAX_POOL_SIZE;
import static net.bluemind.central.reverse.proxy.common.config.CrpConfig.Proxy.MAX_WEB_SOCKETS;
import static net.bluemind.central.reverse.proxy.common.config.CrpConfig.Proxy.PORT;
import static net.bluemind.central.reverse.proxy.common.config.CrpConfig.Proxy.TCP_KEEP_ALIVE;
import static net.bluemind.central.reverse.proxy.common.config.CrpConfig.Proxy.TCP_NO_DELAY;
import static net.bluemind.central.reverse.proxy.common.config.CrpConfig.Proxy.Ssl.ACTIVE;
import static net.bluemind.central.reverse.proxy.common.config.CrpConfig.Proxy.Ssl.ENGINE;
import static net.bluemind.central.reverse.proxy.common.config.CrpConfig.Proxy.Ssl.USE_ALPN;
import static net.bluemind.central.reverse.proxy.common.config.CrpConfig.Proxy.Ssl.VERIFY_HOST;
import static net.bluemind.central.reverse.proxy.common.config.CrpConfig.Proxy.Ssl.Engine.OPEN_SSL;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JdkSSLEngineOptions;
import io.vertx.core.net.OpenSSLEngineOptions;
import io.vertx.core.net.SSLEngineOptions;
import net.bluemind.central.reverse.proxy.model.client.ProxyInfoStoreClient;
import net.bluemind.central.reverse.proxy.vertx.AuthMatcher;
import net.bluemind.central.reverse.proxy.vertx.HttpProxy;
import net.bluemind.central.reverse.proxy.vertx.HttpServerRequestContext;
import net.bluemind.central.reverse.proxy.vertx.SessionManager;
import net.bluemind.central.reverse.proxy.vertx.WebSocketProxy;

public class ProxyVerticle extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(ProxyVerticle.class);

	private final Config config;
	private final SessionManager sessions;
	private MessageConsumer<JsonObject> vertxConsumer;

	public ProxyVerticle(Config config, SessionManager sessions) {
		this.config = config;
		this.sessions = sessions;
	}

	@Override
	public void start(Promise<Void> p) {
		logger.info("[proxy:{}] Starting", deploymentID());
		vertxConsumer = vertx.eventBus().<JsonObject>consumer(ADDRESS).handler(event -> {
			if (MODEL_READY_NAME.equals(event.headers().get("action"))) {
				logger.info("[proxy:{}] Model ready, starting verticle instance proxy", deploymentID());
				startProxy(sessions);
				logger.info("[proxy:{}] Started on port {}", deploymentID(), config.getInt(PORT));
			} else if (INSTALLATION_IP_CHANGE_NAME.equals(event.headers().get("action"))) {
				logger.info("[proxy:{}] Installation IP has change, closing remaining sessions", deploymentID());
				sessions.close(event.body().getString("ip"));
			}
		});
		p.complete();
	}

	private void startProxy(SessionManager sessions) {
		HttpClient proxyClient = vertx.createHttpClient(httpClientOptions());

		HttpProxy httpProxy = HttpProxy.reverseProxy(deploymentID(), proxyClient);
		AuthMatcher<HttpServerRequestContext> requestInfoMatcher = AuthMatcher.requestMatcher();
		ProxyInfoStoreClient storeClient = ProxyInfoStoreClient.create(vertx);
		httpProxy.originSelector(new DownstreamSelector<>(requestInfoMatcher, storeClient, sessions));
		httpProxy.responseHook(CompositeResponseHook
				.of(Arrays.asList(new LoginCookieHook(requestInfoMatcher), new ProxyLogHook(requestInfoMatcher))));

		WebSocketProxy webSocketProxy = WebSocketProxy.reverseProxy(deploymentID(), proxyClient);
		AuthMatcher<ServerWebSocket> webSocketInfoMatcher = AuthMatcher.webSocketMatcher();
		webSocketProxy.originSelector(new DownstreamSelector<>(webSocketInfoMatcher, storeClient, sessions));

		HttpServer proxyServer = vertx.createHttpServer();
		proxyServer.requestHandler(httpProxy).webSocketHandler(webSocketProxy).listen(config.getInt(PORT));

	}

	private HttpClientOptions httpClientOptions() {
		SSLEngineOptions sslEngineOptions = (config.getString(ENGINE).toUpperCase().equals(OPEN_SSL.name()))
				? new OpenSSLEngineOptions()
				: new JdkSSLEngineOptions();
		return new HttpClientOptions() //
				.setKeepAlive(config.getBoolean(KEEP_ALIVE)).setTcpKeepAlive(config.getBoolean(TCP_KEEP_ALIVE))
				.setTcpNoDelay(config.getBoolean(TCP_NO_DELAY)).setMaxPoolSize(config.getInt(MAX_POOL_SIZE))
				.setMaxWebSockets(config.getInt(MAX_WEB_SOCKETS)) //
				.setSsl(config.getBoolean(ACTIVE)) //
				.setUseAlpn(config.getBoolean(USE_ALPN)) //
				.setSslEngineOptions(sslEngineOptions) //
				.setVerifyHost(config.getBoolean(VERIFY_HOST));
	}

	public void tearDown() {
		if (vertxConsumer != null) {
			vertxConsumer.unregister();
		}
	}
}
