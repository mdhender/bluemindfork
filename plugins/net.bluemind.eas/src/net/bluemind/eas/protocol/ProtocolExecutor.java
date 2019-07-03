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
package net.bluemind.eas.protocol;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.w3c.dom.Document;

import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.http.AuthorizedDeviceQuery;
import net.bluemind.eas.impl.vertx.compat.SessionWrapper;
import net.bluemind.eas.impl.vertx.compat.VertxResponder;
import net.bluemind.eas.protocol.impl.ExecutionPayload;
import net.bluemind.lib.vertx.BlockingCode;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.vertx.common.LocalJsonObject;

public final class ProtocolExecutor {

	private static final Logger logger = LoggerFactory.getLogger(ProtocolExecutor.class);
	private static final EventBus eb = VertxPlatform.eventBus();

	private static final ExecutorService backendInit = Executors.newFixedThreadPool(4);

	public static <Q, R> void run(AuthorizedDeviceQuery query, Document document, final IEasProtocol<Q, R> protocol) {
		if (logger.isDebugEnabled()) {
			logger.debug("[{}] Running protocol {}", query.loginAtDomain(), protocol);
		}
		query.request().pause();
		CompletableFuture<BackendSession> futureSession = BlockingCode.forVertx(query.vertx()).withExecutor(backendInit)
				.run(() -> SessionWrapper.wrap(query));
		futureSession.whenComplete((bs, e) -> {
			if (e != null) {
				errorOut(query, e);
			} else {
				RequestDTOHandler<Q, R> requestHandler = new RequestDTOHandler<Q, R>(bs, query, protocol);
				try {
					protocol.parse(query.optionalParams(), document, bs, requestHandler);
				} catch (Exception ex) {
					errorOut(query, ex);
				}
			}
		});
	}

	private static void errorOut(AuthorizedDeviceQuery query, Throwable e) {
		logger.error(e.getMessage(), e);
		query.request().resume();
		HttpServerResponse resp = query.request().response();
		resp.setStatusCode(500).setStatusMessage(String.format("Throwable: %s", e.getMessage())).end();
	}

	private static class RequestDTOHandler<Q, R> implements Handler<Q> {

		private final IEasProtocol<Q, R> protocol;
		private final HttpServerRequest vertxReq;
		private final BackendSession bs;
		private final Vertx vertx;

		public RequestDTOHandler(BackendSession bs, AuthorizedDeviceQuery query, IEasProtocol<Q, R> protocol) {
			this.protocol = protocol;
			this.vertxReq = query.request();
			this.bs = bs;
			this.vertx = query.vertx();

		}

		@Override
		public void handle(Q protocolQuery) {
			if (logger.isDebugEnabled()) {
				logger.debug("Got parsed request: {}", protocolQuery);
			}

			ProtocolCircuitBreaker.INSTANCE.applyCall(vertx, bs, () -> {
				callProtocol(protocolQuery);
				return null;
			}).whenComplete((v, ex) -> {
				if (ex != null) {
					MDC.put("user", bs.getLoginAtDomain().replace("@", "_at_"));
					logger.error("[{}] Completion error", bs.getLoginAtDomain(), ex);
					MDC.put("user", "anonymous");
				}
			});

		}

		private void callProtocol(Q protocolQuery) {
			LocalJsonObject<ExecutionPayload<Q>> payload = new LocalJsonObject<ExecutionPayload<Q>>(
					new ExecutionPayload<Q>(bs, protocolQuery));
			MDC.put("user", bs.getLoginAtDomain().replace("@", "_at_"));
			eb.send(protocol.address(), payload, new Handler<Message<LocalJsonObject<AsyncResult<R>>>>() {

				@Override
				public void handle(Message<LocalJsonObject<AsyncResult<R>>> protoResponseMsg) {
					MDC.put("user", bs.getLoginAtDomain().replace("@", "_at_"));
					AsyncResult<R> asyncResult = protoResponseMsg.body().getValue();
					vertxReq.resume();
					if (asyncResult.succeeded()) {
						VertxResponder responder = new VertxResponder(vertxReq, vertxReq.response(), vertx);
						try {
							protocol.write(bs, responder, asyncResult.result(), new Handler<Void>() {

								@Override
								public void handle(Void event) {
									MDC.put("user", bs.getLoginAtDomain().replace("@", "_at_"));
									MDC.put("user", "anonymous");
								}
							});
						} catch (Exception e) {
							logger.error(e.getMessage(), e);
							HttpServerResponse resp = vertxReq.response();
							resp.setStatusCode(500).setStatusMessage(e.getMessage() != null ? e.getMessage() : "null")
									.end();
						}
					} else {
						Throwable t = asyncResult.cause();
						logger.error(t.getMessage(), t);
						HttpServerResponse resp = vertxReq.response();
						resp.setStatusCode(500).setStatusMessage(String.format("Throwable: %s", t.getMessage())).end();
					}
					MDC.put("user", "anonymous");
				}
			});
			MDC.put("user", "anonymous");
		}

	}

}
