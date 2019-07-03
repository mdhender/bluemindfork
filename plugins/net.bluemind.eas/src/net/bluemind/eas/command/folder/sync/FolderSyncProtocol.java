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
package net.bluemind.eas.command.folder.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.w3c.dom.Document;

import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.FolderChangeReference;
import net.bluemind.eas.backend.FolderChanges;
import net.bluemind.eas.backend.IBackend;
import net.bluemind.eas.backend.IHierarchyExporter;
import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.base.ChangeType;
import net.bluemind.eas.dto.foldersync.FolderSyncRequest;
import net.bluemind.eas.dto.foldersync.FolderSyncResponse;
import net.bluemind.eas.dto.foldersync.FolderSyncResponse.Changes.Change;
import net.bluemind.eas.dto.foldersync.FolderSyncResponse.Status;
import net.bluemind.eas.dto.sync.SyncState;
import net.bluemind.eas.dto.type.ItemDataType;
import net.bluemind.eas.impl.Backends;
import net.bluemind.eas.impl.Responder;
import net.bluemind.eas.protocol.IEasProtocol;
import net.bluemind.eas.serdes.IResponseBuilder;
import net.bluemind.eas.serdes.foldersync.FolderSyncRequestParser;
import net.bluemind.eas.serdes.foldersync.FolderSyncResponseFormatter;
import net.bluemind.eas.state.StateMachine;
import net.bluemind.eas.wbxml.builder.WbxmlResponseBuilder;

public class FolderSyncProtocol implements IEasProtocol<FolderSyncRequest, FolderSyncResponse> {

	private static final Logger logger = LoggerFactory.getLogger(FolderSyncProtocol.class);
	private IBackend backend;

	public FolderSyncProtocol() {
		backend = Backends.dataAccess();
	}

	@Override
	public void parse(OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past,
			Handler<FolderSyncRequest> parserResultHandler) {
		if (logger.isDebugEnabled()) {
			logger.debug("******** Parsing *******");
		}

		FolderSyncRequestParser parser = new FolderSyncRequestParser();
		FolderSyncRequest parsed = parser.parse(optParams, doc, past);
		parserResultHandler.handle(parsed);
	}

	@Override
	public void execute(BackendSession bs, FolderSyncRequest query, Handler<FolderSyncResponse> responseHandler) {
		if (query == null) {
			responseHandler.handle(null);
			return;
		}
		logger.info("FolderSync from {}", query.syncKey);

		FolderSyncResponse response = new FolderSyncResponse();

		String syncKey = query.syncKey;
		boolean fullSync = syncKey == null || syncKey.equals("0");
		if (fullSync) {
			Backends.internalStorage().resetFolder(bs);
			bs.clearAll();
		}

		IHierarchyExporter exporter = backend.getHierarchyExporter(bs);
		StateMachine sm = new StateMachine(Backends.internalStorage());

		SyncState state = sm.getFolderSyncState(bs, syncKey);

		if (state == null) {
			logger.error("SyncState is not valid. Send Invalid SyncKey to device: {}, key: {}", bs.getDevId(), syncKey);
			response.status = Status.InvalidSyncKey;
			responseHandler.handle(response);
			return;
		}

		response.status = Status.Success;
		try {
			FolderChanges changes = exporter.getChanges(bs, state);

			for (FolderChangeReference ic : changes.items) {
				if (ic.changeType == ChangeType.ADD) {
					response.changes.add.add(toFolderSyncChange(ic));
				} else if (ic.changeType == ChangeType.CHANGE) {
					response.changes.update.add(toFolderSyncChange(ic));
				} else if (ic.changeType == ChangeType.DELETE) {
					response.changes.delete.add(Long.toString(ic.folderId));
				}
			}

			response.syncKey = sm.generateSyncKey(ItemDataType.FOLDER, changes.version, changes.subscriptionVersion);

			response.changes.count = response.changes.add.size() + response.changes.update.size()
					+ response.changes.delete.size();

			responseHandler.handle(response);

		} catch (Exception e) {
			logger.error("Fail to send FolderHierarchy response", e);
			response = new FolderSyncResponse();
			response.status = Status.ServerError;
			responseHandler.handle(response);
		}
	}

	private Change toFolderSyncChange(FolderChangeReference sf) {
		Change c = new Change();
		c.serverId = Long.toString(sf.folderId);
		c.parentId = Long.toString(sf.parentId);
		c.displayName = sf.displayName;
		c.type = sf.itemType;
		return c;
	}

	@Override
	public void write(BackendSession bs, Responder responder, FolderSyncResponse response,
			final Handler<Void> completion) {
		if (logger.isDebugEnabled()) {
			logger.debug("******** Writing *******");
		}

		if (response == null) {
			responder.sendStatus(400);
			return;
		}

		FolderSyncResponseFormatter format = new FolderSyncResponseFormatter();
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
		return "eas.protocol.foldersync";
	}

}
