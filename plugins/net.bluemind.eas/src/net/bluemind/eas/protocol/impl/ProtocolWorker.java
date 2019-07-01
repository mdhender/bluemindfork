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
package net.bluemind.eas.protocol.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;

import net.bluemind.eas.protocol.IEasProtocol;
import net.bluemind.vertx.common.LocalJsonObject;

public class ProtocolWorker extends BusModBase {

	private static final Logger logger = LoggerFactory.getLogger(ProtocolWorker.class);

	public void start() {
		super.start();
		for (IEasProtocol<?, ?> proto : Protocols.get()) {
			registerProtocol(proto);
		}

	}

	private <Q, R> void registerProtocol(final IEasProtocol<Q, R> protocol) {
		if (logger.isDebugEnabled()) {
			logger.debug("Registering protocol @ {} ({})", protocol.address(), protocol);
		}
		eb.registerHandler(protocol.address(), new Handler<Message<LocalJsonObject<ExecutionPayload<Q>>>>() {

			@Override
			public void handle(final Message<LocalJsonObject<ExecutionPayload<Q>>> queryMsg) {
				ExecutionPayload<Q> payload = queryMsg.body().getValue();
				try {
					MDC.put("user", payload.bs.getLoginAtDomain().replace("@", "_at_"));
					protocol.execute(payload.bs, payload.query, new Handler<R>() {

						@Override
						public void handle(final R protocolResponse) {
							AsyncResult<R> result = new AsyncResult<R>() {

								@Override
								public R result() {
									return protocolResponse;
								}

								@Override
								public Throwable cause() {
									return null;
								}

								@Override
								public boolean succeeded() {
									return true;
								}

								@Override
								public boolean failed() {
									return false;
								}
							};
							queryMsg.reply(new LocalJsonObject<AsyncResult<R>>(result));
						}
					});
					MDC.put("user", "anonymous");
				} catch (final Exception t) {
					AsyncResult<R> result = new AsyncResult<R>() {

						@Override
						public R result() {
							return null;
						}

						@Override
						public Throwable cause() {
							return t;
						}

						@Override
						public boolean succeeded() {
							return false;
						}

						@Override
						public boolean failed() {
							return true;
						}
					};
					queryMsg.reply(new LocalJsonObject<AsyncResult<R>>(result));
				}
			}
		});
	}

}
