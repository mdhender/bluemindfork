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

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import net.bluemind.eas.protocol.IEasProtocol;
import net.bluemind.vertx.common.LocalJsonObject;

public class ProtocolWorker extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(ProtocolWorker.class);

	public void start() {
		for (IEasProtocol<?, ?> proto : Protocols.get()) {
			registerProtocol(proto);
		}

	}

	private <Q, R> void registerProtocol(final IEasProtocol<Q, R> protocol) {
		if (logger.isDebugEnabled()) {
			logger.debug("Registering protocol @ {} ({})", protocol.address(), protocol);
		}
		vertx.eventBus().consumer(protocol.address(),
				(final Message<LocalJsonObject<ExecutionPayload<Q>>> queryMsg) -> {
					ExecutionPayload<Q> payload = queryMsg.body().getValue();
					try {
						MDC.put("user", payload.bs.getLoginAtDomain().replace("@", "_at_"));
						protocol.execute(payload.bs, payload.query,
								(R protocolResponse) -> queryMsg.reply(new LocalJsonObject<R>(protocolResponse)));
						MDC.put("user", "anonymous");
					} catch (final Exception t) {
						queryMsg.fail(500, t.getMessage());
					}
				});
	}

}
