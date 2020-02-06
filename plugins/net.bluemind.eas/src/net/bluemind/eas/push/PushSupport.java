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
package net.bluemind.eas.push;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import net.bluemind.eas.dto.EasBusEndpoints;
import net.bluemind.eas.dto.push.PushRegistrationRequest;
import net.bluemind.eas.dto.push.PushTrigger;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.vertx.common.LocalJsonObject;

public class PushSupport {

	private static final Logger logger = LoggerFactory.getLogger(PushSupport.class);
	private static final EventBus eb = VertxPlatform.eventBus();

	public static void register(String userUid, String latd, long timeoutMs, String partnershipId,
			Set<Integer> collections, Handler<AsyncResult<Message<LocalJsonObject<PushTrigger>>>> replyHandler) {

		MDC.put("user", latd.replace("@", "_at_"));

		PushRegistrationRequest pushReq = new PushRegistrationRequest();
		pushReq.userUid = userUid;
		pushReq.collectionIds = collections;
		pushReq.pushKey = partnershipId;
		pushReq.timeoutMs = timeoutMs;

		LocalJsonObject<PushRegistrationRequest> jso = new LocalJsonObject<>(pushReq);
		logger.info("[{}] Setting up push on collections {}, with a timeout of {}s.", latd, collections,
				pushReq.timeoutMs / 1000);

		eb.request(EasBusEndpoints.PUSH_REGISTRATION, jso, new DeliveryOptions().setSendTimeout(timeoutMs),
				replyHandler);
	}

}
