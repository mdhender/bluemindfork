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
package net.bluemind.eas.command.mail.sendmail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.google.common.io.ByteSource;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.SendMailData;
import net.bluemind.eas.dto.EasBusEndpoints;
import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.dto.sendmail.SendMailRequest;
import net.bluemind.eas.dto.sendmail.SendMailResponse;
import net.bluemind.eas.dto.sendmail.SendMailResponse.Status;
import net.bluemind.eas.impl.Backends;
import net.bluemind.eas.impl.Responder;
import net.bluemind.eas.protocol.IEasProtocol;
import net.bluemind.eas.serdes.IResponseBuilder;
import net.bluemind.eas.serdes.sendmail.SendMailRequestParser;
import net.bluemind.eas.serdes.sendmail.SendMailResponseFormatter;
import net.bluemind.eas.store.ISyncStorage;
import net.bluemind.eas.utils.EasLogUser;
import net.bluemind.eas.wbxml.builder.WbxmlResponseBuilder;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.vertx.common.LocalJsonObject;

public class SendMailProtocol implements IEasProtocol<SendMailRequest, SendMailResponse> {

	private static final Logger logger = LoggerFactory.getLogger(SendMailProtocol.class);

	@Override
	public void parse(BackendSession bs, OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past,
			Handler<SendMailRequest> parserResultHandler) {
		if (logger.isDebugEnabled()) {
			EasLogUser.logDebugAsUser(bs.getLoginAtDomain(), logger, "******** Parsing *******");
		}

		SendMailRequestParser parser = new SendMailRequestParser();
		SendMailRequest parsed = parser.parse(optParams, doc, past, bs.getLoginAtDomain());
		parserResultHandler.handle(parsed);
	}

	@Override
	public void execute(BackendSession bs, final SendMailRequest query,
			final Handler<SendMailResponse> responseHandler) {
		if (logger.isDebugEnabled()) {
			EasLogUser.logDebugAsUser(bs.getLoginAtDomain(), logger, "******** Executing *******");
		}
		final ISyncStorage store = Backends.internalStorage();

		if (!store.isKnownClientId(query.clientId)) {
			// send
			byte[] bytes = java.util.Base64.getDecoder().decode(query.mime);

			SendMailData mail = new SendMailData();
			mail.backendSession = bs;
			mail.mailContent = ByteSource.wrap(bytes);
			mail.saveInSent = query.saveInSentItems;

			VertxPlatform.eventBus().request(EasBusEndpoints.SEND_MAIL, new LocalJsonObject<SendMailData>(mail),
					(AsyncResult<Message<String>> event) -> {
						if (event.failed()) {
							SendMailResponse response = new SendMailResponse();
							response.status = Status.MAIL_SUBMISSION_FAILED;
							responseHandler.handle(response);
							return;
						}
						String resp = event.result().body();

						if (resp == null) {
							store.insertClientId(query.clientId);
							responseHandler.handle(null);
						} else {
							Status status = Status.valueOf(resp);
							SendMailResponse response = new SendMailResponse();
							response.status = status;
							responseHandler.handle(response);
						}

					});

		} else {
			EasLogUser.logWarnAsUser(bs.getLoginAtDomain(), logger, "Skipping duplicate send {} for {}", query.clientId,
					bs.getLoginAtDomain());
			SendMailResponse response = new SendMailResponse();
			response.status = Status.PREVIOUSLY_SENT;
			responseHandler.handle(response);
		}

	}

	@Override
	public void write(BackendSession bs, Responder responder, SendMailResponse response,
			final Handler<Void> completion) {
		if (logger.isDebugEnabled()) {
			EasLogUser.logDebugAsUser(bs.getLoginAtDomain(), logger, "******** Writing *******");
		}
		if (response == null) {
			responder.sendStatus(200);
			completion.handle(null);
		} else {
			SendMailResponseFormatter formatter = new SendMailResponseFormatter();
			IResponseBuilder builder = new WbxmlResponseBuilder(bs.getProtocolVersion(), bs.getLoginAtDomain(),
					responder.asOutput());
			formatter.format(builder, bs.getProtocolVersion(), response, data -> completion.handle(null));
		}
	}

	@Override
	public String address() {
		return "eas.protocol.sendmail";
	}

}
