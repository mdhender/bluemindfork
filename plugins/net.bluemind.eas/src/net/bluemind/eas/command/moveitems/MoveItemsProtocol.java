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
package net.bluemind.eas.command.moveitems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import io.vertx.core.Handler;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.HierarchyNode;
import net.bluemind.eas.backend.IBackend;
import net.bluemind.eas.backend.MoveSourceAndDestination;
import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.base.CollectionItem;
import net.bluemind.eas.dto.moveitems.MoveItemsRequest;
import net.bluemind.eas.dto.moveitems.MoveItemsResponse;
import net.bluemind.eas.dto.moveitems.MoveItemsResponse.Response.Status;
import net.bluemind.eas.dto.type.ItemDataType;
import net.bluemind.eas.exception.ActiveSyncException;
import net.bluemind.eas.exception.CollectionNotFoundException;
import net.bluemind.eas.impl.Backends;
import net.bluemind.eas.impl.Responder;
import net.bluemind.eas.protocol.IEasProtocol;
import net.bluemind.eas.serdes.IResponseBuilder;
import net.bluemind.eas.serdes.moveitems.MoveItemsFormatter;
import net.bluemind.eas.serdes.moveitems.MoveItemsParser;
import net.bluemind.eas.store.ISyncStorage;
import net.bluemind.eas.wbxml.builder.WbxmlResponseBuilder;

/**
 * Handles the MoveItems cmd
 * 
 * 
 */
public class MoveItemsProtocol implements IEasProtocol<MoveItemsRequest, MoveItemsResponse> {

	private static final Logger logger = LoggerFactory.getLogger(MoveItemsProtocol.class);
	private final IBackend backend;
	private final ISyncStorage store;
	private BackendSession bs;

	public MoveItemsProtocol() {
		backend = Backends.dataAccess();
		store = Backends.internalStorage();
	}

	@Override
	public void parse(OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past,
			Handler<MoveItemsRequest> parserResultHandler) {
		if (logger.isDebugEnabled()) {
			logger.debug("******** Parsing *******");
		}
		MoveItemsParser parser = new MoveItemsParser();
		MoveItemsRequest parsed = parser.parse(optParams, doc, past);
		parserResultHandler.handle(parsed);
	}

	@Override
	public void execute(BackendSession bs, MoveItemsRequest query, Handler<MoveItemsResponse> responseHandler) {
		if (logger.isDebugEnabled()) {
			logger.debug("******** Executing *******");
		}

		this.bs = bs;

		Multimap<MoveSourceAndDestination, CollectionItem> toMove = HashMultimap.create();
		Map<String, Optional<HierarchyNode>> folderCache = new HashMap<String, Optional<HierarchyNode>>();

		MoveItemsResponse response = new MoveItemsResponse();
		response.moveItems = new ArrayList<>(query.moveItems.size());
		for (MoveItemsRequest.Move item : query.moveItems) {
			if (item.srcFldId.equals(item.dstFldId)) {
				logger.error("Same source and destination collection id");
				appendResponseError(response, item,
						MoveItemsResponse.Response.Status.SameSourceAndDestinationCollectionId);
				continue;
			}

			Optional<HierarchyNode> srcFolder = folderCache.computeIfAbsent(item.srcFldId, this::getAndCheckFolder);

			if (!srcFolder.isPresent()) {
				appendResponseError(response, item, MoveItemsResponse.Response.Status.InvalidSourceCollectionId);
				continue;
			}

			Optional<HierarchyNode> dstFolder = folderCache.computeIfAbsent(item.dstFldId, this::getAndCheckFolder);
			if (!dstFolder.isPresent()) {
				appendResponseError(response, item, MoveItemsResponse.Response.Status.InvalidDestinationCollectionId);
				continue;
			}

			toMove.put(MoveSourceAndDestination.create(srcFolder.get(), dstFolder.get()),
					CollectionItem.of(item.srcMsgId));
		}

		toMove.asMap().forEach((folders, items) -> {
			ItemDataType dataClass = ItemDataType.getValue(folders.getSource().containerType);
			List<MoveItemsResponse.Response> res = new ArrayList<MoveItemsResponse.Response>(items.size());

			try {
				res = backend.getContentsImporter(bs).importMoveItems(bs, dataClass, folders.getSource(),
						folders.getDestination(), new ArrayList<CollectionItem>(items));
			} catch (ActiveSyncException e) {
				logger.error(e.getMessage(), e);
				for (CollectionItem ci : items) {
					MoveItemsResponse.Response r = new MoveItemsResponse.Response();
					r.srcMsgId = folders.getSource().collectionId + ":" + ci.itemId;
					r.dstMsgId = r.srcMsgId;
					r.status = Status.SourceOrDestinationLocked;
					res.add(r);
				}
			}

			response.moveItems.addAll(res);

		});

		responseHandler.handle(response);

	}

	private Optional<HierarchyNode> getAndCheckFolder(String id) {
		HierarchyNode f;
		try {
			f = store.getHierarchyNode(bs, Integer.parseInt(id));
		} catch (CollectionNotFoundException e) {
			return Optional.empty();
		}

		if (ItemDataType.getValue(f.containerType) == ItemDataType.CONTACTS) {
			return Optional.empty();
		}

		return Optional.of(f);
	}

	private void appendResponseError(MoveItemsResponse response, MoveItemsRequest.Move item,
			MoveItemsResponse.Response.Status status) {
		MoveItemsResponse.Response resp = new MoveItemsResponse.Response();
		resp.srcMsgId = item.srcMsgId;
		resp.status = status;
		// GLAG-26: add DstMsgId == SourceMessageId to prevent
		// infinite loop
		resp.dstMsgId = item.srcMsgId;
		response.moveItems.add(resp);
	}

	@Override
	public void write(BackendSession bs, Responder responder, MoveItemsResponse response,
			final Handler<Void> completion) {
		if (logger.isDebugEnabled()) {
			logger.debug("******** Writing *******");
		}
		MoveItemsFormatter formatter = new MoveItemsFormatter();
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
		return "eas.protocol.moveitems";
	}

}
