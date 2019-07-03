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
package net.bluemind.eas.command.resolverecipients;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.w3c.dom.Document;

import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.IBackend;
import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.resolverecipients.ResolveRecipientsRequest;
import net.bluemind.eas.dto.resolverecipients.ResolveRecipientsResponse;
import net.bluemind.eas.dto.resolverecipients.ResolveRecipientsResponse.Response.Recipient;
import net.bluemind.eas.dto.resolverecipients.ResolveRecipientsResponse.Status;
import net.bluemind.eas.impl.Backends;
import net.bluemind.eas.impl.Responder;
import net.bluemind.eas.protocol.IEasProtocol;
import net.bluemind.eas.serdes.IResponseBuilder;
import net.bluemind.eas.serdes.resolverecipients.ResolveRecipientsRequestParser;
import net.bluemind.eas.serdes.resolverecipients.ResolveRecipientsResponseFormatter;
import net.bluemind.eas.wbxml.builder.WbxmlResponseBuilder;

public class ResolveRecipientsProtocol implements IEasProtocol<ResolveRecipientsRequest, ResolveRecipientsResponse> {

	private static final Logger logger = LoggerFactory.getLogger(ResolveRecipientsProtocol.class);

	@Override
	public void parse(OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past,
			Handler<ResolveRecipientsRequest> parserResultHandler) {
		if (logger.isDebugEnabled()) {
			logger.debug("******** Parsing *******");
		}
		ResolveRecipientsRequestParser parser = new ResolveRecipientsRequestParser();
		ResolveRecipientsRequest parsed = parser.parse(optParams, doc, past);
		parserResultHandler.handle(parsed);
	}

	@Override
	public void execute(BackendSession bs, ResolveRecipientsRequest query,
			Handler<ResolveRecipientsResponse> responseHandler) {
		if (logger.isDebugEnabled()) {
			logger.debug("******** Executing *******");
		}

		IBackend backend = Backends.dataAccess();

		ResolveRecipientsResponse response = new ResolveRecipientsResponse();

		response.status = Status.Success;

		List<Recipient> recipients = backend.getContentsExporter(bs).resolveRecipients(bs, query.to,
				query.options.picture);

		response.responses = new ArrayList<ResolveRecipientsResponse.Response>(recipients.size());

		for (Recipient recip : recipients) {
			ResolveRecipientsResponse.Response r = new ResolveRecipientsResponse.Response();
			r.recipients = new ArrayList<ResolveRecipientsResponse.Response.Recipient>(1);
			r.status = ResolveRecipientsResponse.Response.Status.Success;
			r.recipientCount = 1;
			r.to = recip.to;
			r.recipients.add(recip);
			if (query.options.availability != null) {
				recip.availability = backend.getContentsExporter(bs).fetchAvailability(bs, recip.entryUid,
						query.options.availability.startTime, query.options.availability.endTime);
			}
			response.responses.add(r);
		}

		responseHandler.handle(response);

	}

	@Override
	public void write(BackendSession bs, Responder responder, ResolveRecipientsResponse response,
			final Handler<Void> completion) {
		ResolveRecipientsResponseFormatter formatter = new ResolveRecipientsResponseFormatter();
		IResponseBuilder builder = new WbxmlResponseBuilder(bs.getLoginAtDomain(), responder.asOutput());
		formatter.format(builder, bs.getProtocolVersion(), response, new Callback<Void>() {

			@Override
			public void onResult(Void data) {
				completion.handle(null);
			}
		});
	}

	@Override
	public String address() {
		return "eas.protocol.resolverecipients";
	}

}
