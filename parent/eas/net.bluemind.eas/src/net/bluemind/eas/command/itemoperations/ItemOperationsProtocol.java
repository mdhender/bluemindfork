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
package net.bluemind.eas.command.itemoperations;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import io.vertx.core.Handler;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.HierarchyNode;
import net.bluemind.eas.backend.IBackend;
import net.bluemind.eas.backend.IContentsExporter;
import net.bluemind.eas.backend.IContentsImporter;
import net.bluemind.eas.backend.ItemChangeReference;
import net.bluemind.eas.backend.MSAttachementData;
import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.dto.base.AirSyncBaseResponse;
import net.bluemind.eas.dto.base.AppData;
import net.bluemind.eas.dto.base.BodyOptions;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.base.CollectionItem;
import net.bluemind.eas.dto.base.LazyLoaded;
import net.bluemind.eas.dto.email.AttachmentResponse;
import net.bluemind.eas.dto.itemoperations.ItemOperationsRequest;
import net.bluemind.eas.dto.itemoperations.ItemOperationsRequest.EmptyFolderContents;
import net.bluemind.eas.dto.itemoperations.ItemOperationsRequest.Fetch;
import net.bluemind.eas.dto.itemoperations.ItemOperationsRequest.ItemOperation;
import net.bluemind.eas.dto.itemoperations.ItemOperationsRequest.Move;
import net.bluemind.eas.dto.itemoperations.ItemOperationsResponse;
import net.bluemind.eas.dto.itemoperations.ItemOperationsResponse.Response;
import net.bluemind.eas.dto.itemoperations.ItemOperationsResponse.Status;
import net.bluemind.eas.dto.itemoperations.ResponseStyle;
import net.bluemind.eas.dto.search.StoreName;
import net.bluemind.eas.dto.sync.CollectionId;
import net.bluemind.eas.dto.type.ItemDataType;
import net.bluemind.eas.exception.ActiveSyncException;
import net.bluemind.eas.exception.CollectionNotFoundException;
import net.bluemind.eas.exception.NotAllowedException;
import net.bluemind.eas.exception.ObjectNotFoundException;
import net.bluemind.eas.impl.Backends;
import net.bluemind.eas.impl.Responder;
import net.bluemind.eas.impl.vertx.VertxLazyLoader;
import net.bluemind.eas.protocol.IEasProtocol;
import net.bluemind.eas.serdes.IResponseBuilder;
import net.bluemind.eas.serdes.itemoperations.ItemOperationsFormatter;
import net.bluemind.eas.serdes.itemoperations.ItemOperationsParser;
import net.bluemind.eas.store.ISyncStorage;
import net.bluemind.eas.utils.EasLogUser;
import net.bluemind.eas.wbxml.WbxmlOutput;
import net.bluemind.eas.wbxml.builder.WbxmlResponseBuilder;

public class ItemOperationsProtocol implements IEasProtocol<ItemOperationsRequest, ItemOperationsResponse> {

	private static final Logger logger = LoggerFactory.getLogger(ItemOperationsProtocol.class);

	private final IBackend backend;
	private final ISyncStorage store;

	public ItemOperationsProtocol() {
		backend = Backends.dataAccess();
		store = Backends.internalStorage();
	}

	@Override
	public void parse(BackendSession bs, OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past,
			Handler<ItemOperationsRequest> parserResultHandler) {
		ItemOperationsParser parser = new ItemOperationsParser();
		ItemOperationsRequest request = parser.parse(optParams, doc, past, bs.getLoginAtDomain());
		parserResultHandler.handle(request);
	}

	@Override
	public void execute(BackendSession bs, ItemOperationsRequest query,
			Handler<ItemOperationsResponse> responseHandler) {
		ItemOperationsResponse response = new ItemOperationsResponse();
		response.style = query.style;
		response.gzip = query.gzip;
		response.status = Status.SUCCESS;
		for (ItemOperation op : query.itemOperations) {
			ItemOperationsResponse.Response resp = null;
			if (op instanceof ItemOperationsRequest.EmptyFolderContents) {
				resp = emptyFolderContents((ItemOperationsRequest.EmptyFolderContents) op, bs);
			} else if (op instanceof ItemOperationsRequest.Fetch) {
				resp = fetch((ItemOperationsRequest.Fetch) op, bs);
			} else if (op instanceof ItemOperationsRequest.Move) {
				resp = move((ItemOperationsRequest.Move) op, bs);
			} else {
				EasLogUser.logWarnAsUser(bs.getLoginAtDomain(), logger, "unsupported itemsOperations : {}",
						op.getClass());
			}
			if (resp != null) {
				if (resp.status != Status.SUCCESS) {
					resp.status = Status.PARTIAL_SUCCESS;
				}
				response.responses.add(resp);
			}

		}
		EasLogUser.logInfoAsUser(bs.getLoginAtDomain(), logger, "****** Responding with {}", response);
		responseHandler.handle(response);
	}

	private ItemOperationsResponse.Move move(Move op, BackendSession bs) {
		// prot 14.0++
		ItemOperationsResponse.Move respOp = new ItemOperationsResponse.Move();
		respOp.conversationId = op.conversationId;
		respOp.status = ItemOperationsResponse.Status.SUCCESS;

		String[] convId = op.conversationId.split(":");

		HierarchyNode sourceFolder = null;
		try {
			sourceFolder = store.getHierarchyNode(bs, CollectionId.of(convId[0]));
		} catch (Exception e) {
			EasLogUser.logExceptionAsUser(bs.getLoginAtDomain(), e, logger);
			respOp.status = ItemOperationsResponse.Status.SERVER_ERROR;
			return respOp;
		}

		HierarchyNode destinationFolder = null;
		try {
			destinationFolder = store.getHierarchyNode(bs, CollectionId.of(op.dstFldId));
		} catch (Exception e) {
			EasLogUser.logExceptionAsUser(bs.getLoginAtDomain(), e, logger);
			respOp.status = ItemOperationsResponse.Status.SERVER_ERROR;
			return respOp;
		}

		IContentsImporter importer = backend.getContentsImporter(bs);
		ItemDataType type = ItemDataType.getValue(sourceFolder.containerType);

		try {
			importer.importMoveItems(bs, type, sourceFolder, destinationFolder,
					Arrays.asList(CollectionItem.of(op.conversationId)));
		} catch (Exception e) {
			EasLogUser.logExceptionAsUser(bs.getLoginAtDomain(), e, logger);
			respOp.status = ItemOperationsResponse.Status.SERVER_ERROR;
		}

		return respOp;
	}

	private ItemOperationsResponse.EmptyFolderContents emptyFolderContents(EmptyFolderContents op, BackendSession bs) {
		boolean deleteSubFolders = op.options != null ? op.options.deleteSubFolders : false;
		ItemOperationsResponse.Status status = null;
		try {
			HierarchyNode node = store.getHierarchyNode(bs, op.collectionId);
			backend.getContentsImporter(bs).emptyFolderContent(bs, node, op.collectionId, deleteSubFolders);
			status = ItemOperationsResponse.Status.SUCCESS;
		} catch (CollectionNotFoundException e) {
			// FIXME should we use ObjectNotFound ?
			status = ItemOperationsResponse.Status.RESOURCE_ACCESS_DENIED;
		} catch (NotAllowedException e) {
			status = ItemOperationsResponse.Status.RESOURCE_ACCESS_DENIED;
		}

		ItemOperationsResponse.EmptyFolderContents opResp = new ItemOperationsResponse.EmptyFolderContents();
		opResp.status = status;
		opResp.collectionId = op.collectionId.getValue();

		return opResp;
	}

	@Override
	public void write(BackendSession bs, final Responder responder, final ItemOperationsResponse response,
			final Handler<Void> completion) {
		ItemOperationsFormatter format = new ItemOperationsFormatter();
		if (response.style == ResponseStyle.INLINE) {
			WbxmlOutput output = responder.asOutput();
			IResponseBuilder builder = new WbxmlResponseBuilder(bs.getProtocolVersion(), bs.getLoginAtDomain(), output);
			format.format(builder, bs.getProtocolVersion(), response, data -> completion.handle(null));
		} else {
			final ByteArrayOutputStream forWbxml = new ByteArrayOutputStream();
			IResponseBuilder wbxmlBuilder = new WbxmlResponseBuilder(bs.getProtocolVersion(), bs.getLoginAtDomain(),
					WbxmlOutput.of(forWbxml));
			format.format(wbxmlBuilder, bs.getProtocolVersion(), response, data -> {
				MultipartBuilder multipart = new MultipartBuilder();
				multipart.wbxml(forWbxml.toByteArray());
				for (Response r : response.responses) {
					if (r instanceof ItemOperationsResponse.Fetch) {
						ItemOperationsResponse.Fetch fetchResp = (net.bluemind.eas.dto.itemoperations.ItemOperationsResponse.Fetch) r;
						multipart.asyncPart(fetchResp.properties.body);
					}
				}
				multipart.build(responder, completion);
			});
		}

	}

	@Override
	public String address() {
		return "eas.protocol.itemoperations";
	}

	private ItemOperationsResponse.Fetch fetch(Fetch op, BackendSession bs) {
		ItemOperationsResponse.Fetch opResp = null;
		StoreName storeName = StoreName.valueOf(op.store);
		if (StoreName.mailbox.equals(storeName)) {
			try {
				opResp = processMailboxFetch(bs, op);
			} catch (Exception e) {
				EasLogUser.logExceptionAsUser(bs.getLoginAtDomain(), e, logger);
				opResp = new ItemOperationsResponse.Fetch();
				opResp.status = ItemOperationsResponse.Status.ACTION_NOT_SUPPORTED;
			}
		} else {
			EasLogUser.logWarnAsUser(bs.getLoginAtDomain(), logger,
					"ItemOperations is not implemented for store {}. Send status 156 ActionNotSupported", storeName);
			opResp = new ItemOperationsResponse.Fetch();
			opResp.status = ItemOperationsResponse.Status.ACTION_NOT_SUPPORTED;
		}
		return opResp;
	}

	private ItemOperationsResponse.Fetch processMailboxFetch(BackendSession bs, ItemOperationsRequest.Fetch fetchOp) {
		IContentsExporter exporter = backend.getContentsExporter(bs);
		String serverId = fetchOp.serverId;

		// FIXME maybe there is more than one. Maybe there is Zero ?
		String longId = fetchOp.longId;

		if (longId != null) {
			long l = Long.parseLong(fetchOp.longId);
			String cId = Integer.toString((int) (l >> 32));
			return processItemFetch(fetchOp, bs, exporter, cId);
		} else if (fetchOp.fileReference != null) {
			return processFileReferenceFetch(bs, exporter, fetchOp);
		} else if (fetchOp.collectionId != null && serverId != null) {
			return processCollectionFetch(bs, exporter, fetchOp.collectionId, serverId, fetchOp.options.bodyOptions);
		} else {
			ItemOperationsResponse.Fetch fetchResp = new ItemOperationsResponse.Fetch();
			fetchResp.status = Status.ACTION_NOT_SUPPORTED;
			return fetchResp;
		}
	}

	private ItemOperationsResponse.Fetch processItemFetch(Fetch fetchOp, BackendSession bs, IContentsExporter exporter,
			String collectionId) {
		ItemOperationsResponse.Fetch resp = new ItemOperationsResponse.Fetch();
		try {

			// BM-6567 : ItemOperation with LongId
			CollectionItem ci = null;
			if (fetchOp.longId != null) {
				long l = Long.parseLong(fetchOp.longId);
				String colId = Integer.toString((int) (l >> 32));
				String itemId = Integer.toString((int) l);
				ci = CollectionItem.of(colId, Long.parseLong(itemId));
			} else {
				ci = CollectionItem.of(collectionId, Long.parseLong(fetchOp.serverId));
			}

			ItemChangeReference itemRef = new ItemChangeReference(ItemDataType.EMAIL);
			itemRef.setServerId(ci);

			ItemOperationsResponse.Status status = ItemOperationsResponse.Status.SUCCESS;

			resp.status = status;
			resp.longId = fetchOp.serverId;
			resp.dataClass = "Email";

			Optional<AppData> optData = itemRef.getData();
			AppData loaded = optData.orElse(null);
			if (!optData.isPresent()) {
				loaded = exporter.loadStructure(bs, fetchOp.options.bodyOptions, itemRef);
				loaded.body = VertxLazyLoader.wrap(loaded.body);
			}
			resp.properties = loaded;
		} catch (ActiveSyncException e) {
			resp.status = ItemOperationsResponse.Status.SERVER_ERROR;
			return resp;
		}

		return resp;
	}

	private ItemOperationsResponse.Fetch processCollectionFetch(BackendSession bs, IContentsExporter exporter,
			CollectionId collectionId, String serverId, BodyOptions bodyOptions) {

		ItemOperationsResponse.Fetch resp = new ItemOperationsResponse.Fetch();
		resp.serverId = serverId;
		resp.collectionId = collectionId.getValue();

		ItemOperationsResponse.Status status = ItemOperationsResponse.Status.SUCCESS;
		try {
			HierarchyNode node = store.getHierarchyNode(bs, collectionId);
			ItemDataType dataType = ItemDataType.getValue(node.containerType);
			resp.dataClass = dataType.asXmlValue();

			ItemChangeReference itemRef = new ItemChangeReference(dataType);
			itemRef.setServerId(CollectionItem.of(serverId));

			AppData loaded = exporter.loadStructure(bs, bodyOptions, itemRef);
			loaded.body = VertxLazyLoader.wrap(loaded.body);
			resp.properties = loaded;
		} catch (ActiveSyncException e) {
			EasLogUser.logExceptionAsUser(bs.getLoginAtDomain(), e, logger);
			status = ItemOperationsResponse.Status.SERVER_ERROR;

		}

		resp.status = status;
		if (status != ItemOperationsResponse.Status.SUCCESS) {
			return resp;
		}

		return resp;
	}

	private ItemOperationsResponse.Fetch processFileReferenceFetch(final BackendSession bs,
			final IContentsExporter exporter, final ItemOperationsRequest.Fetch fetchOp) {
		ItemOperationsResponse.Fetch resp = new ItemOperationsResponse.Fetch();
		resp.fileReference = fetchOp.fileReference;

		AttachmentResponse ar = null;
		try {
			ar = exporter.getAttachmentMetadata(bs, fetchOp.fileReference);
		} catch (ObjectNotFoundException e) {
			resp.status = ItemOperationsResponse.Status.ATTACHEMENT_INVALID;
			return resp;
		}

		LazyLoaded<BodyOptions, AirSyncBaseResponse> lazy = new LazyLoaded<BodyOptions, AirSyncBaseResponse>(null) {

			@Override
			public void load(Callback<AirSyncBaseResponse> onLoad) {
				try {
					MSAttachementData data = exporter.getAttachment(bs, fetchOp.fileReference);
					AirSyncBaseResponse content = new AirSyncBaseResponse();
					content.body = new AirSyncBaseResponse.Body();
					content.contentType = data.getContentType();
					content.body.data = data.getFile();
					EasLogUser.logInfoAsUser(bs.getLoginAtDomain(), logger, "Finished async loading of {} attachment.",
							content.contentType);
					onLoad.onResult(content);
				} catch (Exception e) {
					EasLogUser.logExceptionAsUser(bs.getLoginAtDomain(), e, logger);
					onLoad.onResult(null);
				}
			}
		};

		resp.status = ItemOperationsResponse.Status.SUCCESS;
		resp.properties = AppData.of(ar, VertxLazyLoader.wrap(lazy), fetchOp.options);
		return resp;
	}

}
