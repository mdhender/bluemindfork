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
package net.bluemind.eas.command.folder.crud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.w3c.dom.Document;

import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.IBackend;
import net.bluemind.eas.backend.IHierarchyImporter;
import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.folderdelete.FolderDeleteRequest;
import net.bluemind.eas.dto.folderdelete.FolderDeleteResponse;
import net.bluemind.eas.dto.folderdelete.FolderDeleteResponse.Status;
import net.bluemind.eas.dto.type.ItemDataType;
import net.bluemind.eas.exception.CollectionNotFoundException;
import net.bluemind.eas.impl.Backends;
import net.bluemind.eas.impl.Responder;
import net.bluemind.eas.protocol.IEasProtocol;
import net.bluemind.eas.serdes.IResponseBuilder;
import net.bluemind.eas.serdes.folderdelete.FolderDeleteRequestParser;
import net.bluemind.eas.serdes.folderdelete.FolderDeleteResponseFormatter;
import net.bluemind.eas.state.StateMachine;
import net.bluemind.eas.store.ISyncStorage;
import net.bluemind.eas.wbxml.builder.WbxmlResponseBuilder;

public class FolderDeleteProtocol implements IEasProtocol<FolderDeleteRequest, FolderDeleteResponse> {

	private static final Logger logger = LoggerFactory.getLogger(FolderDeleteProtocol.class);
	private final IBackend backend;
	private final ISyncStorage store;

	public FolderDeleteProtocol() {
		backend = Backends.dataAccess();
		store = Backends.internalStorage();
	}

	@Override
	public void parse(OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past,
			Handler<FolderDeleteRequest> parserResultHandler) {
		if (logger.isDebugEnabled()) {
			logger.debug("******** Parsing *******");
		}

		FolderDeleteRequestParser parser = new FolderDeleteRequestParser();
		FolderDeleteRequest parsed = parser.parse(optParams, doc, past);
		parserResultHandler.handle(parsed);

	}

	@Override
	public void execute(BackendSession bs, FolderDeleteRequest query, Handler<FolderDeleteResponse> responseHandler) {
		if (logger.isDebugEnabled()) {
			logger.debug("******** Executing *******");
		}

		FolderDeleteResponse response = new FolderDeleteResponse();

		Integer serverId = null;
		try {
			serverId = Integer.parseInt(query.serverId);
		} catch (NumberFormatException e) {
			logger.error("Invalid serverId {}", query.serverId);
			response.status = Status.InvalidRequest;
			responseHandler.handle(response);
			return;
		}

		try {
			store.getHierarchyNode(bs, serverId);
		} catch (CollectionNotFoundException e1) {
			logger.error("ServerId {} does not exist", query.serverId);
			response.status = Status.DoesNotExist;
			responseHandler.handle(response);
			return;
		}

		IHierarchyImporter importer = backend.getHierarchyImporter(bs);
		boolean deleted = importer.importFolderDelete(bs, serverId);

		if (deleted) {
			StateMachine sm = new StateMachine(store);
			response.status = Status.Success;
			response.syncKey = sm.generateSyncKey(ItemDataType.FOLDER);
			responseHandler.handle(response);
		} else {
			response.status = Status.ServerError;
			responseHandler.handle(response);
		}

	}

	@Override
	public void write(BackendSession bs, Responder responder, FolderDeleteResponse response,
			final Handler<Void> completion) {
		if (logger.isDebugEnabled()) {
			logger.debug("******** Writing *******");
		}

		FolderDeleteResponseFormatter format = new FolderDeleteResponseFormatter();
		IResponseBuilder builder = new WbxmlResponseBuilder(bs.getLoginAtDomain(), responder.asOutput());
		format.format(builder, bs.getProtocolVersion(), response, new Callback<Void>() {

			@Override
			public void onResult(Void data) {
				completion.handle(null);
			}
		});

	}

	@Override
	public String address() {
		return "eas.protocol.folderdelete";
	}

}
