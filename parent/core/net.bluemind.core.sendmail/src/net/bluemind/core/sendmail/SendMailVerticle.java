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
package net.bluemind.core.sendmail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.LocalJsonObject;

public class SendMailVerticle extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(SendMailVerticle.class);

	@Override
	public void start() {

		final ISendmail mailer = new Sendmail();
		vertx.eventBus().consumer(SendMailAddress.SEND, new Handler<Message<LocalJsonObject<Mail>>>() {
			public void handle(Message<LocalJsonObject<Mail>> message) {
				try {
					mailer.send(message.body().getValue());
				} catch (ServerFault e) {
					logger.error(e.getMessage(), e);
				}

			}
		});

	}
}
