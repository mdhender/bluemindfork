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
package net.bluemind.eas.command.getitemestimate;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.w3c.dom.Document;

import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.getitemestimate.GetItemEstimateRequest;
import net.bluemind.eas.dto.getitemestimate.GetItemEstimateRequest.Collection;
import net.bluemind.eas.dto.getitemestimate.GetItemEstimateResponse;
import net.bluemind.eas.dto.getitemestimate.GetItemEstimateResponse.Response.Status;
import net.bluemind.eas.dto.sync.SyncState;
import net.bluemind.eas.exception.CollectionNotFoundException;
import net.bluemind.eas.impl.Backends;
import net.bluemind.eas.impl.Responder;
import net.bluemind.eas.protocol.IEasProtocol;
import net.bluemind.eas.serdes.IResponseBuilder;
import net.bluemind.eas.serdes.getitemestimate.GetItemEstimateRequestParser;
import net.bluemind.eas.serdes.getitemestimate.GetItemEstimateResponseFormatter;
import net.bluemind.eas.state.StateMachine;
import net.bluemind.eas.store.ISyncStorage;
import net.bluemind.eas.wbxml.builder.WbxmlResponseBuilder;

public class GetItemEstimateProtocol implements IEasProtocol<GetItemEstimateRequest, GetItemEstimateResponse> {

	private static final Logger logger = LoggerFactory.getLogger(GetItemEstimateProtocol.class);

	@Override
	public void parse(OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past,
			Handler<GetItemEstimateRequest> parserResultHandler) {
		if (logger.isDebugEnabled()) {
			logger.debug("******** Parsing *******");
		}

		GetItemEstimateRequestParser parser = new GetItemEstimateRequestParser();
		GetItemEstimateRequest parsed = parser.parse(optParams, doc, past);
		parserResultHandler.handle(parsed);
	}

	@Override
	public void execute(BackendSession bs, GetItemEstimateRequest query,
			Handler<GetItemEstimateResponse> responseHandler) {
		if (logger.isDebugEnabled()) {
			logger.debug("******** Executing *******");
		}

		ISyncStorage store = Backends.internalStorage();
		StateMachine sm = new StateMachine(store);

		GetItemEstimateResponse response = new GetItemEstimateResponse();
		response.responses = new ArrayList<GetItemEstimateResponse.Response>(query.collections.size());
		for (Collection c : query.collections) {
			GetItemEstimateResponse.Response r = new GetItemEstimateResponse.Response();
			r.collectionId = c.collectionId;

			try {
				Integer collectionId = Integer.parseInt(c.collectionId);
				store.getHierarchyNode(bs, collectionId);
				SyncState state = sm.getSyncState(bs, collectionId, c.syncKey);
				if (state == null) {
					r.status = Status.InvalidSyncKey;
				} else {
					r.status = Status.Success;
					bs.addLastClientSyncState(collectionId, state);
				}
			} catch (CollectionNotFoundException e) {
				r.status = Status.InvalidCollection;
			}

			response.responses.add(r);
		}

		responseHandler.handle(response);
	}

	@Override
	public void write(BackendSession bs, Responder responder, GetItemEstimateResponse response,
			final Handler<Void> completion) {
		if (logger.isDebugEnabled()) {
			logger.debug("******** Writing *******");
		}
		GetItemEstimateResponseFormatter formatter = new GetItemEstimateResponseFormatter();
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
		return "eas.protocol.getitemestimate";
	}

}
