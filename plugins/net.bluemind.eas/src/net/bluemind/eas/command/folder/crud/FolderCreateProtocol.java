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

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import io.vertx.core.Handler;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.FolderChangeReference;
import net.bluemind.eas.backend.HierarchyNode;
import net.bluemind.eas.backend.IBackend;
import net.bluemind.eas.backend.IHierarchyImporter;
import net.bluemind.eas.backend.SyncFolder;
import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.base.ChangeType;
import net.bluemind.eas.dto.foldercreate.FolderCreateRequest;
import net.bluemind.eas.dto.foldercreate.FolderCreateResponse;
import net.bluemind.eas.dto.foldercreate.FolderCreateResponse.Status;
import net.bluemind.eas.dto.foldersync.FolderType;
import net.bluemind.eas.dto.type.ItemDataType;
import net.bluemind.eas.exception.CollectionNotFoundException;
import net.bluemind.eas.impl.Backends;
import net.bluemind.eas.impl.Responder;
import net.bluemind.eas.protocol.IEasProtocol;
import net.bluemind.eas.serdes.IResponseBuilder;
import net.bluemind.eas.serdes.foldercreate.FolderCreateRequestParser;
import net.bluemind.eas.serdes.foldercreate.FolderCreateResponseFormatter;
import net.bluemind.eas.state.StateMachine;
import net.bluemind.eas.store.ISyncStorage;
import net.bluemind.eas.wbxml.builder.WbxmlResponseBuilder;

public class FolderCreateProtocol implements IEasProtocol<FolderCreateRequest, FolderCreateResponse> {

	private static final Logger logger = LoggerFactory.getLogger(FolderCreateProtocol.class);
	private IBackend backend;

	public FolderCreateProtocol() {
		backend = Backends.dataAccess();
	}

	@Override
	public void parse(OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past,
			Handler<FolderCreateRequest> parserResultHandler) {
		if (logger.isDebugEnabled()) {
			logger.debug("******** Parsing *******");
		}

		FolderCreateRequestParser parser = new FolderCreateRequestParser();
		FolderCreateRequest parsed = parser.parse(optParams, doc, past);
		parserResultHandler.handle(parsed);
	}

	@Override
	public void execute(BackendSession bs, FolderCreateRequest query, Handler<FolderCreateResponse> responseHandler) {
		if (logger.isDebugEnabled()) {
			logger.debug("******** Executing *******");
		}

		FolderCreateResponse response = new FolderCreateResponse();
		ISyncStorage store = Backends.internalStorage();

		String displayName = query.displayName;
		int parentId = Integer.parseInt(query.parentId);

		HierarchyNode parent = null;
		if (parentId > 0) {
			try {
				parent = store.getHierarchyNode(bs, parentId);
			} catch (CollectionNotFoundException e1) {
				logger.error("Cannot create folder '{}', parent id {} not found", displayName, query.parentId);
				response.status = Status.ParentFolderNotFound;
				responseHandler.handle(response);
				return;
			}
		}

		ItemDataType pim = getItemDataType(query.type);

		if (pim == null) {
			logger.error("Cannot create folder '{}', unsupported type: {} ({})", displayName, query.type,
					FolderType.getValue(query.type));
			response.status = Status.InvalidRequest;
			responseHandler.handle(response);
			return;
		}

		IHierarchyImporter importer = backend.getHierarchyImporter(bs);

		SyncFolder sf = new SyncFolder();
		sf.setPimDataType(pim);
		sf.setParentId(parentId);
		sf.setDisplayName(displayName);

		Long serverId = importer.importFolderCreate(bs, parent, sf);

		if (serverId != null) {
			StateMachine sm = new StateMachine(store);

			response.status = Status.Success;
			response.serverId = Long.toString(serverId);
			response.syncKey = sm.generateSyncKey(ItemDataType.FOLDER);

			List<FolderChangeReference> sentToDevice = new LinkedList<FolderChangeReference>();
			FolderChangeReference ic = new FolderChangeReference();
			ic.changeType = ChangeType.ADD;
			ic.itemType = FolderType.getValue(query.type);
			ic.folderId = serverId;
			ic.parentId = parentId;
			ic.displayName = displayName;
			sentToDevice.add(ic);

			responseHandler.handle(response);
		} else {
			logger.error("Fail to create folder '{}', type: {} ({})", displayName, query.type,
					FolderType.getValue(query.type));
			response = new FolderCreateResponse();
			response.status = Status.ServerError;
			responseHandler.handle(response);
		}
	}

	private ItemDataType getItemDataType(int type) {
		FolderType ft = FolderType.getValue(type);
		if (isMailFodler(ft)) {
			return ItemDataType.EMAIL;
		} else if (ft == FolderType.USER_CREATED_TASKS_FOLDER) {
			return ItemDataType.TASKS;
		} else if (ft == FolderType.USER_CREATED_CALENDAR_FOLDER) {
			return ItemDataType.CALENDAR;
		}
		return null;
	}

	private boolean isMailFodler(FolderType type) {
		return (type == FolderType.DEFAULT_INBOX_FOLDER || type == FolderType.DEFAULT_DRAFTS_FOLDERS
				|| type == FolderType.DEFAULT_DELETED_ITEMS_FOLDERS || type == FolderType.DEFAULT_SENT_EMAIL_FOLDER
				|| type == FolderType.DEFAULT_OUTBOX_FOLDER || type == FolderType.USER_CREATED_EMAIL_FOLDER);
	}

	@Override
	public void write(BackendSession bs, Responder responder, FolderCreateResponse response,
			final Handler<Void> completion) {
		FolderCreateResponseFormatter format = new FolderCreateResponseFormatter();
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
		return "eas.protocol.foldercreate";
	}

}
