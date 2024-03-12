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
import net.bluemind.eas.utils.EasLogUser;

public final class ProtocolExecutor {

	private static final Logger logger = LoggerFactory.getLogger(ProtocolExecutor.class);

	private ProtocolExecutor() {
	}

	public static <Q, R> void run(AuthorizedDeviceQuery query, Document document, final IEasProtocol<Q, R> protocol) {
		if (logger.isDebugEnabled()) {
			logger.debug("[{}] Running protocol {}", query.loginAtDomain(), protocol);
		}
		query.request().pause();

		query.vertx().executeBlocking(() -> SessionWrapper.wrap(query)).andThen(r -> {
			if (r.failed()) {
				errorOut(query, r.cause());
			} else {
				RequestDTOHandler<Q, R> requestHandler = new RequestDTOHandler<>(r.result(), query, protocol);
				try {
					protocol.parse(r.result(), query.optionalParams(), document, r.result(), requestHandler);
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
					EasLogUser.logErrorExceptionAsUser(bs.getLoginAtDomain(), ex, logger, "[{}] Completion error",
							bs.getLoginAtDomain());
				}
			});

		}

		private void callProtocol(Q protocolQuery) {
			String mdcVal = bs.getLoginAtDomain();
			vertx.executeBlocking((Promise<R> rProm) -> {
				vertxReq.exceptionHandler(rProm::tryFail);
				try {
					protocol.execute(bs, protocolQuery, rProm::tryComplete);
				} catch (Exception e) {
					rProm.tryFail(e);
				}
			}, false, (AsyncResult<R> res) -> {
				vertxReq.resume();
				if (vertxReq.response().closed()) {
					EasLogUser.logWarnAsUser(mdcVal, logger, "Skip response to closed connection with {}", protocol);
					return;
				}

				if (res.succeeded()) {
					VertxResponder responder = new VertxResponder(vertxReq, vertxReq.response(), vertx);
					try {
						protocol.write(bs, responder, res.result(), handler -> {
							MDC.put("user", bs.getLoginAtDomain().replace("@", "_at_"));
							MDC.put("user", "anonymous");
						});
					} catch (Exception e) {
						failSilently(mdcVal, e);
					}
				} else {
					failSilently(mdcVal, res.cause());
				}
			});
		}

		private void failSilently(String user, Throwable e) {
			try {
				EasLogUser.logExceptionAsUser(user, e, logger);
				HttpServerResponse resp = vertxReq.response();
				resp.setStatusCode(500).setStatusMessage(e.getMessage() != null ? e.getMessage() : "null").end();
			} catch (Exception ex) {
				// don't report if the connection is already closed
			}
		}

	}

}
