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
package net.bluemind.eas.command.mail.smartreply;

import java.util.Collection;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.SendMailData;
import net.bluemind.eas.backend.SendMailData.Mode;
import net.bluemind.eas.command.mail.MailRequestBase;
import net.bluemind.eas.dto.EasBusEndpoints;
import net.bluemind.eas.http.AuthorizedDeviceQuery;
import net.bluemind.eas.http.IEasRequestEndpoint;
import net.bluemind.eas.impl.Responder;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.vertx.common.LocalJsonObject;

/**
 * Handles the SmartReply cmd
 */
public class SmartReplyEndpoint12 extends MailRequestBase implements IEasRequestEndpoint {

	public SmartReplyEndpoint12() {

	}

	@Override
	public void process(AuthorizedDeviceQuery dq, BackendSession bs, ByteSource mailContent, boolean saveInSent,
			Responder responder, Handler<Void> completion) {
		SendMailData mail = new SendMailData();
		mail.backendSession = bs;
		mail.mailContent = mailContent;
		mail.saveInSent = saveInSent;
		mail.collectionId = dq.optionalParams().collectionId();
		mail.serverId = dq.optionalParams().itemId();
		mail.mode = Mode.Reply;

		VertxPlatform.eventBus().request(EasBusEndpoints.SEND_MAIL, new LocalJsonObject<SendMailData>(mail),
				(AsyncResult<Message<String>> event) -> {

					if (event.failed()) {
						completion.handle(null);
						responder.sendStatus(500);
					} else if (event.result().body() == null) {
						completion.handle(null);
						responder.sendStatus(200);
					} else {
						completion.handle(null);
						responder.sendStatus(500);
					}

				});
	}

	@Override
	public Collection<String> supportedCommands() {
		return ImmutableList.of("SmartReply");
	}

	@Override
	public boolean acceptsVersion(double protocolVersion) {
		return protocolVersion < 14;
	}
}
