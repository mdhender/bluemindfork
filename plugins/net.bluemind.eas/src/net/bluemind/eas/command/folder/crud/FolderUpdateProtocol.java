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
import org.w3c.dom.Document;

import io.vertx.core.Handler;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.IBackend;
import net.bluemind.eas.backend.IHierarchyImporter;
import net.bluemind.eas.backend.SyncFolder;
import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.folderupdate.FolderUpdateRequest;
import net.bluemind.eas.dto.folderupdate.FolderUpdateResponse;
import net.bluemind.eas.dto.folderupdate.FolderUpdateResponse.Status;
import net.bluemind.eas.dto.sync.CollectionId;
import net.bluemind.eas.dto.type.ItemDataType;
import net.bluemind.eas.exception.CollectionNotFoundException;
import net.bluemind.eas.impl.Backends;
import net.bluemind.eas.impl.Responder;
import net.bluemind.eas.protocol.IEasProtocol;
import net.bluemind.eas.serdes.IResponseBuilder;
import net.bluemind.eas.serdes.folderupdate.FolderUpdateRequestParser;
import net.bluemind.eas.serdes.folderupdate.FolderUpdateResponseFormatter;
import net.bluemind.eas.state.StateMachine;
import net.bluemind.eas.store.ISyncStorage;
import net.bluemind.eas.wbxml.builder.WbxmlResponseBuilder;

public class FolderUpdateProtocol implements IEasProtocol<FolderUpdateRequest, FolderUpdateResponse> {

	private static final Logger logger = LoggerFactory.getLogger(FolderUpdateProtocol.class);
	private IBackend backend;

	public FolderUpdateProtocol() {
		backend = Backends.dataAccess();
	}

	@Override
	public void parse(OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past,
			Handler<FolderUpdateRequest> parserResultHandler) {
		if (logger.isDebugEnabled()) {
			logger.debug("******** Parsing *******");
		}

		FolderUpdateRequestParser parser = new FolderUpdateRequestParser();
		FolderUpdateRequest parsed = parser.parse(optParams, doc, past);
		parserResultHandler.handle(parsed);
	}

	@Override
	public void execute(BackendSession bs, FolderUpdateRequest query, Handler<FolderUpdateResponse> responseHandler) {
		if (logger.isDebugEnabled()) {
			logger.debug("******** Executing *******");
		}

		FolderUpdateResponse response = new FolderUpdateResponse();
		ISyncStorage store = Backends.internalStorage();

		String displayName = query.displayName;

		try {
			store.getHierarchyNode(bs, query.serverId);
		} catch (CollectionNotFoundException e1) {
			logger.error("ServerId {} does not exist", query.serverId);
			response.status = Status.DoesNotExist;
			responseHandler.handle(response);
			return;
		}

		CollectionId parentId = query.parentId;
		if (!"0".equals(parentId.getValue())) {
			try {
				store.getHierarchyNode(bs, parentId);
			} catch (CollectionNotFoundException e1) {
				logger.error("Cannot update folder '{}', parent id {} not found", displayName, query.parentId);
				response.status = Status.ParentFolderNotFound;
				responseHandler.handle(response);
				return;
			}
		}

		IHierarchyImporter importer = backend.getHierarchyImporter(bs);

		SyncFolder sf = new SyncFolder();
		sf.setServerId(query.serverId);
		sf.setParentId(parentId);
		sf.setDisplayName(displayName);

		boolean update = importer.importFolderUpdate(bs, sf);
		if (update) {
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
	public void write(BackendSession bs, Responder responder, FolderUpdateResponse response,
			final Handler<Void> completion) {
		if (logger.isDebugEnabled()) {
			logger.debug("******** Writing *******");
		}

		FolderUpdateResponseFormatter format = new FolderUpdateResponseFormatter();
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
		return "eas.protocol.folderupdate";
	}

}
