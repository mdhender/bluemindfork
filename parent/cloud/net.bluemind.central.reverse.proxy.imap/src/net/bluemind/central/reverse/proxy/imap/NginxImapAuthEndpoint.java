/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.central.reverse.proxy.imap;

import static net.bluemind.central.reverse.proxy.common.ProxyEventBusAddress.ADDRESS;
import static net.bluemind.central.reverse.proxy.common.ProxyEventBusAddress.MODEL_READY_NAME;

import java.security.InvalidParameterException;
import java.util.Base64;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import net.bluemind.central.reverse.proxy.model.client.ProxyInfoStoreClient;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.lib.vertx.utils.PasswordDecoder;

public class NginxImapAuthEndpoint extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(NginxImapAuthEndpoint.class);
	private ProxyInfoStoreClient infoClient;

	@Override
	public void start(Promise<Void> p) throws Exception {
		this.infoClient = ProxyInfoStoreClient.create(vertx);
		vertx.eventBus().<JsonObject>consumer(ADDRESS).handler(event -> {
			if (MODEL_READY_NAME.equals(event.headers().get("action"))) {
				logger.info("[proxy:{}] Model ready, starting nginx endpoint", deploymentID());
				startEndpoint();
			}
		});
		p.complete();
	}

	private void startEndpoint() {
		HttpServerOptions opts = new HttpServerOptions()//
				.setTcpFastOpen(true)//
				.setTcpNoDelay(true)//
				.setTcpQuickAck(true);
		vertx.createHttpServer(opts).requestHandler(this::nginxRoutingRequest).listen(8143, ar -> {
			if (ar.succeeded()) {
				var srv = ar.result();
				logger.info("[imap-auth:{}] Started on port {}", srv, srv.actualPort());
			} else {
				logger.info("[imap-auth:{}] Failed to listen on port 8143", ar.cause());
			}
		});
	}

	private static record QueryParameters(String clientIp, String protocol, String user, String latd, String password,
			String backendPort, long time, int attempt) {

		private static QueryParameters fromRequest(HttpServerRequest req, long time) {
			String clientIp = req.headers().get("Client-IP");
			String backendPort = req.headers().get("X-Auth-Port");
			String protocol = req.headers().get("Auth-Protocol");
			int attempt = Optional.ofNullable(req.headers().get("Auth-Login-Attempt")).map(Integer::parseInt).orElse(0);

			String user = req.headers().get("Auth-User");
			if (user == null || "".equals(user)) {
				throw new InvalidParameterException("null or empty login");
			}

			user = new String(decode(user)).toLowerCase();
			String latd = user;

			String password = PasswordDecoder.getPassword(user, decode(req.headers().get("Auth-Pass")));
			if (logger.isDebugEnabled()) {
				logger.debug("Password b64: {}, decoded: {}", req.headers().get("Auth-Pass"), password);
			}

			return new QueryParameters(clientIp, protocol, user, latd, password, backendPort, time, attempt);
		}
	}

	/**
	 * @param b64
	 * @return
	 */
	public static byte[] decode(String b64) {
		return Base64.getDecoder().decode(b64);
	}

	/**
	 * <code>curl -v -H "Auth-User: dG9tQGRldmVudi5ibHVl" -H "Auth-Pass: dG9t" http://localhost:8143</code>
	 * 
	 * for tom@devenv.blue / tom
	 * 
	 * @param req
	 */
	private void nginxRoutingRequest(HttpServerRequest req) {
		long time = System.nanoTime();
		req.endHandler(v -> {
			var resp = req.response();
			try {
				QueryParameters qp = QueryParameters.fromRequest(req, time);
				infoClient.ip(qp.user()).flatMap(s -> {
					if (s == null) {
						return Future.failedFuture("unknown user");
					} else {
						return Future.succeededFuture(s);
					}
				}).onSuccess(imapEndpointIp -> {
					MultiMap respHeaders = resp.headers();

					respHeaders.add("Auth-Status", "OK");
					respHeaders.add("Auth-Server", imapEndpointIp);
					respHeaders.add("Auth-Port", "1143");
					resp.end();
				}).onFailure(ex -> {
					logger.warn("No routing infos for login '{}'", qp.user);
					MultiMap respHeaders = resp.headers();
					respHeaders.add("Auth-Status", "Invalid login or password");
					if (qp.attempt() < 10) {
						respHeaders.add("Auth-Wait", "2");
					}
					resp.end();
				});
			} catch (Exception e) {
				logger.error("Nginx routing error", e);
				resp.setStatusCode(500).end();
			}

		});

	}

	public static final class EndpointFactory implements IVerticleFactory {

		@Override
		public boolean isWorker() {
			return false;
		}

		@Override
		public Verticle newInstance() {
			return new NginxImapAuthEndpoint();
		}

	}

}
