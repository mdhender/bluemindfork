/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.eas.command.find;

import org.w3c.dom.Document;

import io.vertx.core.Handler;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.IBackend;
import net.bluemind.eas.backend.IContentsExporter;
import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.dto.find.FindRequest;
import net.bluemind.eas.dto.find.FindResponse;
import net.bluemind.eas.dto.find.FindResponse.Status;
import net.bluemind.eas.exception.CollectionNotFoundException;
import net.bluemind.eas.impl.Backends;
import net.bluemind.eas.impl.Responder;
import net.bluemind.eas.protocol.IEasProtocol;
import net.bluemind.eas.serdes.IResponseBuilder;
import net.bluemind.eas.serdes.find.FindRequestParser;
import net.bluemind.eas.serdes.find.FindResponseFormatter;
import net.bluemind.eas.wbxml.builder.WbxmlResponseBuilder;

public class FindProtocol implements IEasProtocol<FindRequest, FindResponse> {

	private IBackend backend;

	public FindProtocol() {
		backend = Backends.dataAccess();

	}

	@Override
	public void parse(OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past,
			Handler<FindRequest> parserResultHandler) {
		FindRequestParser parser = new FindRequestParser();
		FindRequest parsed = parser.parse(optParams, doc, past);
		parserResultHandler.handle(parsed);
	}

	@Override
	public void execute(BackendSession bs, FindRequest query, Handler<FindResponse> responseHandler) {
		FindResponse response = new FindResponse();

		IContentsExporter contentsExporter = backend.getContentsExporter(bs);

		try {
			response.response = contentsExporter.find(bs, query);
			response.response.status = Status.SUCCESS;
		} catch (CollectionNotFoundException e) {
			response.status = Status.FOLDER_SYNC_REQUIRED;
			responseHandler.handle(response);
			return;
		}

		response.status = Status.SUCCESS;
		responseHandler.handle(response);
	}

	@Override
	public void write(BackendSession bs, Responder responder, FindResponse response, Handler<Void> completion) {
		FindResponseFormatter formatter = new FindResponseFormatter();
		IResponseBuilder builder = new WbxmlResponseBuilder(bs.getLoginAtDomain(), responder.asOutput());
		formatter.format(builder, bs.getProtocolVersion(), response, data -> completion.handle(null));
	}

	@Override
	public String address() {
		return "eas.protocol.find";
	}

}
