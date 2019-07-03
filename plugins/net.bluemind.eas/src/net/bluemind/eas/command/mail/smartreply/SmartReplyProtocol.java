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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.w3c.dom.Document;

import com.google.common.io.ByteSource;

import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.IBackend;
import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.smartreply.SmartReplyRequest;
import net.bluemind.eas.dto.smartreply.SmartReplyResponse;
import net.bluemind.eas.dto.smartreply.SmartReplyResponse.Status;
import net.bluemind.eas.exception.ActiveSyncException;
import net.bluemind.eas.impl.Backends;
import net.bluemind.eas.impl.Responder;
import net.bluemind.eas.protocol.IEasProtocol;
import net.bluemind.eas.serdes.IResponseBuilder;
import net.bluemind.eas.serdes.smartreply.SmartReplyRequestParser;
import net.bluemind.eas.serdes.smartreply.SmartReplyResponseFormatter;
import net.bluemind.eas.store.ISyncStorage;
import net.bluemind.eas.wbxml.builder.WbxmlResponseBuilder;

public class SmartReplyProtocol implements IEasProtocol<SmartReplyRequest, SmartReplyResponse> {

	private static final Logger logger = LoggerFactory.getLogger(SmartReplyProtocol.class);

	@Override
	public void parse(OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past,
			Handler<SmartReplyRequest> parserResultHandler) {
		if (logger.isDebugEnabled()) {
			logger.debug("******** Parsing *******");
		}

		SmartReplyRequestParser parser = new SmartReplyRequestParser();
		SmartReplyRequest parsed = parser.parse(optParams, doc, past);
		parserResultHandler.handle(parsed);

	}

	@Override
	public void execute(BackendSession bs, SmartReplyRequest query, Handler<SmartReplyResponse> responseHandler) {
		IBackend backend = Backends.dataAccess();
		ISyncStorage store = Backends.internalStorage();
		if (!store.isKnownClientId(query.clientId)) {
			byte[] mailContent = java.util.Base64.getDecoder().decode(query.mime);

			String folderId = query.source.folderId;
			String serverId = query.source.itemId;
			String longId = query.source.longId;
			if (folderId == null && serverId == null && longId != null) {
				long l = Long.parseLong(longId);
				folderId = Integer.toString((int) (l >> 32));
				serverId = folderId + ":" + Integer.toString((int) l);
			}

			try {
				backend.getContentsImporter(bs).replyEmail(bs, ByteSource.wrap(mailContent), query.saveInSentItems,
						folderId, serverId, !query.replaceMime);
				store.insertClientId(query.clientId);

				responseHandler.handle(null);
			} catch (ActiveSyncException e) {
				logger.error("error in smarForward", e);
				SmartReplyResponse response = new SmartReplyResponse();
				response.status = SmartReplyResponse.Status.MessageReplyFailed;
				responseHandler.handle(response);

			}
		} else {
			SmartReplyResponse response = new SmartReplyResponse();
			response.status = Status.PreviouslySent;
			responseHandler.handle(response);
		}
	}

	@Override
	public void write(BackendSession bs, Responder responder, SmartReplyResponse response,
			final Handler<Void> completion) {
		if (logger.isDebugEnabled()) {
			logger.debug("******** Writing *******");
		}
		if (response == null) {
			responder.sendStatus(200);
			completion.handle(null);
		} else {
			SmartReplyResponseFormatter formatter = new SmartReplyResponseFormatter();
			IResponseBuilder builder = new WbxmlResponseBuilder(bs.getLoginAtDomain(), responder.asOutput());
			formatter.format(builder, bs.getProtocolVersion(), response, new Callback<Void>() {

				@Override
				public void onResult(Void data) {
					completion.handle(null);
				}
			});
		}
	}

	@Override
	public String address() {
		return "eas.protocol.smartreply";
	}

}
