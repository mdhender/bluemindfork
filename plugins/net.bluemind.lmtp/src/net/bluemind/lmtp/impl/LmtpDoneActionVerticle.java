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
package net.bluemind.lmtp.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import net.bluemind.lmtp.Activator;
import net.bluemind.lmtp.backend.DeliveredVersion;
import net.bluemind.lmtp.backend.IDeliveryDoneAction;
import net.bluemind.lmtp.backend.LmtpAddress;
import net.bluemind.lmtp.backend.LmtpEnvelope;
import net.bluemind.lmtp.backend.LmtpReply;
import net.bluemind.lmtp.impl.busmessages.DeliveredMailMessage;

public class LmtpDoneActionVerticle extends AbstractVerticle {

	public static final String ADDR = "lmtp.doneActions";

	private static final Logger logger = LoggerFactory.getLogger(LmtpDoneActionVerticle.class);
	private Handler<Message<DeliveredMailMessage>> doneHandler;

	@Override
	public void start() {

		doneHandler = new Handler<Message<DeliveredMailMessage>>() {

			@Override
			public void handle(Message<DeliveredMailMessage> event) {
				done(event.body().getEnvelope());
			}
		};
		getVertx().eventBus().consumer(ADDR, doneHandler);
	}

	protected void done(LmtpEnvelope mEnvelope) {

		int numDelivered = 0;

		List<IDeliveryDoneAction> onDDActions = Activator.getDefault().getDeliveryDoneActions();
		// de-duplicate versions
		Set<DeliveredVersion> alreadyDelivred = new HashSet<>();
		for (LmtpAddress recipient : mEnvelope.getRecipients()) {
			LmtpReply reply = recipient.getDeliveryStatus();

			if (logger.isDebugEnabled()) {
				logger.debug(recipient.getEmailAddress() + " return status: " + reply.toString());
			}

			String rmail = recipient.getEmailAddress();
			if (reply.success()) {
				DeliveredVersion dv = recipient.getDeliveredVersion();
				if (alreadyDelivred.contains(dv)) {
					logger.warn("mail already delivred {}, that should not happen", dv.getMbox());
					continue;
				}
				alreadyDelivred.add(dv);
				logger.info("[{}] On {} mail {}", mEnvelope.getId(), dv.getMbox(), rmail);
				for (IDeliveryDoneAction dda : onDDActions) {
					try {
						dda.newMessageDelivered(mEnvelope, dv, rmail);
					} catch (Exception t) {
						logger.error("Error on delivery done action " + dda, t);
					}
				}
				numDelivered++;
			} else {
				logger.error("[{}] {}: {}", mEnvelope.getId(), rmail, reply);
			}
		}
		for (IDeliveryDoneAction dda : onDDActions) {
			dda.deliveryFinished(mEnvelope);
		}

		if (logger.isDebugEnabled()) {
			logger.debug("delivery count: " + numDelivered);
		}

	}

}
