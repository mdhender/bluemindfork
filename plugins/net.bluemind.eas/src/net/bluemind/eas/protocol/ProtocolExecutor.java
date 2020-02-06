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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.w3c.dom.Document;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.http.AuthorizedDeviceQuery;
import net.bluemind.eas.impl.vertx.compat.SessionWrapper;
import net.bluemind.eas.impl.vertx.compat.VertxResponder;

public final class ProtocolExecutor {

	private static final Logger logger = LoggerFactory.getLogger(ProtocolExecutor.class);

	private ProtocolExecutor() {
	}

	public static <Q, R> void run(AuthorizedDeviceQuery query, Document document, final IEasProtocol<Q, R> protocol) {
		if (logger.isDebugEnabled()) {
			logger.debug("[{}] Running protocol {}", query.loginAtDomain(), protocol);
		}
		query.request().pause();

		query.vertx().executeBlocking((Promise<BackendSession> p) -> {
			try {
				p.complete(SessionWrapper.wrap(query));
			} catch (Exception e) {
				p.fail(e);
			}
		}, r -> {
			if (r.failed()) {
				errorOut(query, r.cause());
			} else {
				RequestDTOHandler<Q, R> requestHandler = new RequestDTOHandler<>(r.result(), query, protocol);
				try {
					protocol.parse(query.optionalParams(), document, r.result(), requestHandler);
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
			String mdcVal = bs.getLoginAtDomain().replace("@", "_at_");
			MDC.put("user", mdcVal);
			vertx.executeBlocking((Promise<R> rProm) -> {
				MDC.put("user", mdcVal);
				try {
					protocol.execute(bs, protocolQuery, rProm::complete);
				} catch (Exception e) {
					rProm.fail(e);
				}
				MDC.put("user", "anonymous");
			}, (AsyncResult<R> res) -> {
				MDC.put("user", mdcVal);
				vertxReq.resume();
				if (res.succeeded()) {
					VertxResponder responder = new VertxResponder(vertxReq, vertxReq.response(), vertx);
					try {
						protocol.write(bs, responder, res.result(), new Handler<Void>() {

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
					Throwable t = res.cause();
					logger.error(t.getMessage(), t);
					HttpServerResponse resp = vertxReq.response();
					resp.setStatusCode(500).setStatusMessage(String.format("Throwable: %s", t.getMessage())).end();
				}
				MDC.put("user", "anonymous");
			});
		}

	}

}
