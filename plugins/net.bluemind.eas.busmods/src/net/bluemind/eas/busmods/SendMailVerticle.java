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
package net.bluemind.eas.busmods;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import net.bluemind.eas.backend.SendMailData;
import net.bluemind.eas.dto.EasBusEndpoints;
import net.bluemind.eas.dto.sendmail.SendMailResponse;
import net.bluemind.eas.exception.ActiveSyncException;
import net.bluemind.eas.impl.Backends;
import net.bluemind.vertx.common.LocalJsonObject;

public class SendMailVerticle extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(SendMailVerticle.class);

	@Override
	public void start() {

		Handler<Message<LocalJsonObject<SendMailData>>> sendMailHandler = new Handler<Message<LocalJsonObject<SendMailData>>>() {

			@Override
			public void handle(final Message<LocalJsonObject<SendMailData>> msg) {
				try {
					SendMailData mail = msg.body().getValue();
					switch (mail.mode) {
					case Send:
						Backends.dataAccess().getContentsImporter(mail.backendSession).sendEmail(mail);
						break;
					case Reply:
						Backends.dataAccess().getContentsImporter(mail.backendSession).replyEmail(mail.backendSession,
								mail.mailContent, mail.saveInSent, mail.collectionId, mail.serverId, true);
						break;
					case Forward:
						Backends.dataAccess().getContentsImporter(mail.backendSession).forwardEmail(mail.backendSession,
								mail.mailContent, mail.saveInSent, mail.collectionId, mail.serverId, true);
						break;
					}
					msg.reply((String) null);
				} catch (ActiveSyncException e) {
					logger.error("error send mail", e);
					msg.reply(SendMailResponse.Status.MailSubmissionFailed.name());
				}

			}
		};
		vertx.eventBus().consumer(EasBusEndpoints.SEND_MAIL, sendMailHandler);

	}

}
