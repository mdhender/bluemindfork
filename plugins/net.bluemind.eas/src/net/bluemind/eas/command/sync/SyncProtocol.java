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
package net.bluemind.eas.command.sync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.collect.Sets;

import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.Changes;
import net.bluemind.eas.backend.HierarchyNode;
import net.bluemind.eas.backend.IApplicationData;
import net.bluemind.eas.backend.IBackend;
import net.bluemind.eas.backend.IContentsExporter;
import net.bluemind.eas.backend.IContentsImporter;
import net.bluemind.eas.backend.ItemChangeReference;
import net.bluemind.eas.data.CalendarDecoder;
import net.bluemind.eas.data.ContactDecoder;
import net.bluemind.eas.data.EmailDecoder;
import net.bluemind.eas.data.IDataDecoder;
import net.bluemind.eas.data.TaskDecoder;
import net.bluemind.eas.dto.EasBusEndpoints;
import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.dto.base.AppData;
import net.bluemind.eas.dto.base.BodyOptions;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.base.ChangeType;
import net.bluemind.eas.dto.base.CollectionItem;
import net.bluemind.eas.dto.push.PushTrigger;
import net.bluemind.eas.dto.sync.CollectionSyncRequest;
import net.bluemind.eas.dto.sync.CollectionSyncResponse;
import net.bluemind.eas.dto.sync.CollectionSyncResponse.ServerChange;
import net.bluemind.eas.dto.sync.CollectionSyncResponse.ServerResponse;
import net.bluemind.eas.dto.sync.CollectionSyncResponse.ServerResponse.Operation;
import net.bluemind.eas.dto.sync.SyncRequest;
import net.bluemind.eas.dto.sync.SyncResponse;
import net.bluemind.eas.dto.sync.SyncState;
import net.bluemind.eas.dto.sync.SyncStatus;
import net.bluemind.eas.dto.type.ItemDataType;
import net.bluemind.eas.exception.ActiveSyncException;
import net.bluemind.eas.exception.CollectionNotFoundException;
import net.bluemind.eas.impl.Backends;
import net.bluemind.eas.impl.Responder;
import net.bluemind.eas.impl.vertx.VertxLazyLoader;
import net.bluemind.eas.protocol.IEasProtocol;
import net.bluemind.eas.protocol.ProtocolCircuitBreaker;
import net.bluemind.eas.push.PushSupport;
import net.bluemind.eas.serdes.IResponseBuilder;
import net.bluemind.eas.serdes.sync.SyncRequestParser;
import net.bluemind.eas.serdes.sync.SyncResponseFormatter;
import net.bluemind.eas.state.StateMachine;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.wbxml.builder.WbxmlResponseBuilder;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.vertx.common.LocalJsonObject;
import net.bluemind.vertx.common.request.Requests;

//<?xml version="1.0" encoding="UTF-8"?>
//<Sync>
//<Collections>
//<Collection>
//<Class>Contacts</Class>
//<SyncKey>ff16677f-ee9c-42dc-a562-709f899c8d31</SyncKey>
//<CollectionId>bm://contacts/user@domain</CollectionId>
//<DeletesAsMoves/>
//<GetChanges/>
//<WindowSize>100</WindowSize>
//<Options>
//<Truncation>4</Truncation>
//<RTFTruncation>4</RTFTruncation>
//<Conflict>1</Conflict>
//</Options>
//</Collection>
//</Collections>
//</Sync>
public class SyncProtocol implements IEasProtocol<SyncRequest, SyncResponse> {

	private static final Logger logger = LoggerFactory.getLogger(SyncProtocol.class);

	private final IBackend backend;
	private final Map<ItemDataType, IDataDecoder> decoders;

	public SyncProtocol() {
		this.backend = Backends.dataAccess();
		this.decoders = new HashMap<ItemDataType, IDataDecoder>();
		decoders.put(ItemDataType.CONTACTS, new ContactDecoder());
		decoders.put(ItemDataType.CALENDAR, new CalendarDecoder());
		decoders.put(ItemDataType.EMAIL, new EmailDecoder());
		decoders.put(ItemDataType.TASKS, new TaskDecoder());
	}

	@Override
	public void parse(OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past,
			Handler<SyncRequest> parserResultHandler) {
		SyncRequest sr = new SyncRequestParser().parse(optParams, doc, past);
		parserResultHandler.handle(sr);
	}

	private boolean shouldFetchChanges(CollectionSyncRequest sc) {
		Boolean inDOM = sc.isGetChanges();
		if (inDOM == null) {
			if ("0".equals(sc.getSyncKey())) {
				return false;
			} else {
				return true;
			}
		} else {
			return inDOM;
		}

	}

	@Override
	public void execute(final BackendSession bs, final SyncRequest sr, final Handler<SyncResponse> responseHandler) {
		if (sr.collections.isEmpty()) {
			sendError(responseHandler, SyncStatus.PARTIAL_REQUEST);
			return;
		}

		if (sr.waitIntervalSeconds != null && sr.heartbeatInterval != null) {
			sendError(responseHandler, SyncStatus.PROTOCOL_ERROR);
			return;
		}

		if (sr.heartbeatInterval != null) {
			int maxInterval = 1130;
			String maxString = Backends.internalStorage().getSystemConf("eas_max_heartbeat");
			if (maxString != null) {
				try {
					maxInterval = Integer.parseInt(maxString);
				} catch (NumberFormatException nfe) {
					logger.error("Invalid heartbeat value: " + maxString);
				}
			}
			if (sr.heartbeatInterval > maxInterval) {
				logger.error("Invalid HeartbeatInterval {} > {}", sr.heartbeatInterval, maxInterval);
				sendLimitError(responseHandler, maxInterval);
				return;
			}
		}

		if (sr.waitIntervalSeconds != null && sr.waitIntervalSeconds > 59 * 60) {
			sendLimitError(responseHandler, 59);
			return;
		}

		if (bs.getLastMonitored() == null || bs.getLastMonitored().isEmpty()) {
			bs.setLastMonitored(sr.collections);
		}

		// tom: when push is enabled, can we have client changes ?
		// assume NO
		if (sr.waitIntervalSeconds == null) {
			JsonObject jso = new JsonObject();
			for (CollectionSyncRequest sc : sr.collections) {
				jso.putString(sc.getCollectionId().toString(), bs.getDeviceId().getInternalId());
			}
			EventBus eb = VertxPlatform.eventBus();
			eb.send(EasBusEndpoints.PUSH_KILLER, jso, new Handler<Message<Void>>() {
				@Override
				public void handle(Message<Void> event) {
					executeSync(bs, sr, responseHandler);
				}
			});
		} else {
			logger.info("Sync push mode. user: {}, device: {}, collections size: {}", bs.getLoginAtDomain(),
					bs.getDevId(), bs.getLastMonitored().size());

			bs.setLastWaitSeconds(sr.waitIntervalSeconds);
			Set<Integer> cols = new HashSet<>(sr.collections.size());
			for (CollectionSyncRequest sc : sr.collections) {
				cols.add(sc.getCollectionId());
			}
			Requests.tagAsync(bs.getRequest());
			Requests.tag(bs.getRequest(), "timeout", sr.waitIntervalSeconds + "s");

			for (CollectionSyncRequest csr : bs.getLastMonitored()) {
				cols.add(csr.getCollectionId());
			}

			PushSupport.register(bs.getUser().getUid(), bs.getLoginAtDomain(), sr.waitIntervalSeconds * 1000,
					bs.getDeviceId().getInternalId(), cols,
					new SyncReplyHandler(this, bs, responseHandler, Sets.newHashSet(bs.getLastMonitored())));
		}

	}

	private void executeSync(BackendSession bs, SyncRequest sr, Handler<SyncResponse> responseHandler) {
		SyncResponse syncResponse = new SyncResponse();
		int syncErrors = 0;

		for (CollectionSyncRequest sc : sr.collections) {
			CollectionSyncResponse csr = new CollectionSyncResponse();
			csr.collectionId = sc.getCollectionId();

			try {
				// ensure the collectionExists

				HierarchyNode f = Backends.internalStorage().getHierarchyNode(bs, csr.collectionId);
				ItemDataType dataClass = ItemDataType.getValue(f.containerType);

				List<ServerResponse> clientChangeResults = executeClientCommands(bs, sc, dataClass);

				List<String> clientAddedServerIds = new ArrayList<String>(clientChangeResults.size());
				List<ServerResponse> clientConflictedServerIds = new ArrayList<ServerResponse>(
						clientChangeResults.size());

				List<ServerResponse> clientErrorServerIds = new ArrayList<ServerResponse>(clientChangeResults.size());

				for (ServerResponse ssr : clientChangeResults) {
					if (ssr.ackStatus == SyncStatus.OK) {
						clientAddedServerIds.add(ssr.item.toString());
					} else if (ssr.ackStatus == SyncStatus.CONFLICT) {
						clientConflictedServerIds.add(ssr);
					} else if (ssr.ackStatus == SyncStatus.SERVER_ERROR) {
						clientErrorServerIds.add(ssr);
					}
				}

				CollectionChanges serverChanges = serverChanges(bs, sc, clientAddedServerIds);

				sc.getChangedItems().clear();
				sc.getDeletedIds().clear();
				sc.getCreatedItems().clear();
				sc.getFetchIds().clear();

				csr.commands = serverChanges.commands;
				csr.responses = clientChangeResults;
				csr.forceResponse = sc.forceResponse;

				List<String> commands = serverChanges.commands.stream().map(cmd -> cmd.item.itemId)
						.collect(Collectors.toList());
				if (!clientConflictedServerIds.isEmpty()) {
					IContentsExporter contentExporter = backend.getContentsExporter(bs);
					for (ServerResponse sse : clientConflictedServerIds) {
						if (commands.contains(sse.item.itemId)) {
							// duplicate command
							continue;
						}
						ServerChange conflicted = new ServerChange();
						ItemChangeReference icr = new ItemChangeReference(dataClass);
						icr.setServerId(sse.item);
						AppData data = contentExporter.loadStructure(bs, null, icr);
						conflicted.data = Optional.of(data);
						if (sse.operation == Operation.Change) {
							conflicted.type = ServerChange.ChangeType.Change;
						} else if (sse.operation == Operation.Delete) {
							conflicted.type = ServerChange.ChangeType.Add;
						}
						conflicted.item = sse.item;
						csr.commands.add(conflicted);
					}
				}

				if (!clientErrorServerIds.isEmpty()) {
					for (ServerResponse sse : clientErrorServerIds) {
						sse.ackStatus = SyncStatus.OK;
						sse.operation = Operation.Add;
						ServerChange err = new ServerChange();
						err.item = sse.item;
						err.type = ServerChange.ChangeType.Delete;
						err.data = Optional.empty();
						csr.commands.add(err);
					}
				}
				csr.status = serverChanges.status;
				csr.syncKey = serverChanges.syncKey;
				csr.moreAvailable = serverChanges.moreAvailable;
			} catch (CollectionNotFoundException cnf) {
				logger.error(cnf.getMessage(), cnf);

				// Sync OK to prevent android synchronization loop
				csr.status = SyncStatus.OK;
				csr.syncKey = sc.getSyncKey();
				csr.commands = Collections.emptyList();
				csr.responses = Collections.emptyList();

			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				csr.syncKey = sc.getSyncKey();
				csr.status = SyncStatus.SERVER_ERROR;
				syncErrors++;
			}

			syncResponse.collections.add(csr);
		}

		if (syncErrors > 0) {
			ProtocolCircuitBreaker.INSTANCE.noticeError(bs);
		} else {
			ProtocolCircuitBreaker.INSTANCE.noticeSuccess(bs);
		}

		boolean empty = syncResponse.collections.stream()
				.allMatch(c -> c.commands.isEmpty() && c.responses.isEmpty() && !c.forceResponse);
		if (empty && syncErrors == 0) {
			responseHandler.handle(null);
		} else {
			responseHandler.handle(syncResponse);
		}

	}

	@Override
	public void write(final BackendSession bs, final Responder responder, SyncResponse response,
			final Handler<Void> completion) {

		if (response == null) {
			// IN-31, delayed empty response
			responder.vertx().setTimer(500, h -> {
				Backends.internalStorage().updateLastSync(bs);
				responder.sendStatus(200);
				completion.handle(null);
			});
			return;
		}

		Callback<Void> cb = new Callback<Void>() {
			@Override
			public void onResult(Void dom) {
				Backends.internalStorage().updateLastSync(bs);
				completion.handle(null);
			}
		};
		IResponseBuilder builder = new WbxmlResponseBuilder(bs.getLoginAtDomain(), responder.asOutput());
		SyncResponseFormatter srf = new SyncResponseFormatter();
		srf.format(builder, bs.getProtocolVersion(), response, cb);
	}

	@Override
	public String address() {
		return "eas.protocol.sync";
	}

	private static class SyncReplyHandler implements Handler<AsyncResult<Message<LocalJsonObject<PushTrigger>>>> {

		private final Handler<SyncResponse> r;
		private final Set<CollectionSyncRequest> collections;
		private final BackendSession bs;
		private final SyncProtocol endpoint;

		public SyncReplyHandler(SyncProtocol syncEndpoint, BackendSession bs, Handler<SyncResponse> r,
				Set<CollectionSyncRequest> set) {
			this.r = r;
			this.bs = bs;
			this.collections = set;
			this.endpoint = syncEndpoint;
		}

		@Override
		public void handle(AsyncResult<Message<LocalJsonObject<PushTrigger>>> event) {
			MDC.put("user", bs.getLoginAtDomain().replace("@", "_at_"));
			if (event.failed()) {
				logger.info("[{}] Sync timed-out", bs.getLoginAtDomain());
				endpoint.sendError(r, SyncStatus.NEED_RETRY);
			} else {
				PushTrigger pt = event.result().body().getValue();
				if (pt.noChanges) {
					SyncResponse syncResponse = new SyncResponse();
					for (CollectionSyncRequest sc : collections) {
						CollectionSyncResponse csr = new CollectionSyncResponse();
						csr.collectionId = sc.getCollectionId();
						csr.status = SyncStatus.OK;
						csr.syncKey = sc.getSyncKey();
						syncResponse.collections.add(csr);
					}
					r.handle(syncResponse);
				} else {
					SyncResponse syncResponse = new SyncResponse();
					for (CollectionSyncRequest sc : collections) {
						CollectionSyncResponse csr = new CollectionSyncResponse();
						csr.collectionId = sc.getCollectionId();
						CollectionChanges serverChanges = endpoint.serverChanges(bs, sc, new ArrayList<String>());
						csr.commands = serverChanges.commands;
						csr.status = serverChanges.status;
						csr.syncKey = serverChanges.syncKey;
						csr.moreAvailable = serverChanges.moreAvailable;
						syncResponse.collections.add(csr);
					}
					r.handle(syncResponse);
				}
			}
			MDC.put("user", "anonymous");
		}

	}

	/**
	 * Returns the list of items for which we sent an 'Add' command.
	 * 
	 */
	private Changes doSync(BackendSession bs, CollectionSyncRequest c, SyncState state,
			List<String> clientAddedServerIds) throws ActiveSyncException {

		Changes changes = new Changes();

		IContentsExporter contentExporter = backend.getContentsExporter(bs);

		if (bs.getUnSynchronizedItemChange(c.getCollectionId()).size() == 0) {
			changes = contentExporter.getChanged(bs, state, c.options.filterType, c.getCollectionId());
		} else {
			changes.version = state.version;
		}

		if (!clientAddedServerIds.isEmpty()) {
			Iterator<ItemChangeReference> it = changes.items.iterator();
			while (it.hasNext()) {
				ItemChangeReference ir = it.next();
				if (ir.getChangeType() != ChangeType.DELETE
						&& clientAddedServerIds.contains(ir.getServerId().toString())) {
					it.remove();
				}
			}
		}

		changes = applyWindowSize(c, changes, bs);
		c.addedAndUpdated += changes.items.size();

		BodyOptions baseRequest = asBaseRequest(c);

		List<String> toLoad = changes.items.stream().filter(
				change -> (change.getChangeType() == ChangeType.ADD || change.getChangeType() == ChangeType.CHANGE))
				.map(i -> i.getServerId().itemId).collect(Collectors.toList());

		if (!toLoad.isEmpty()) {
			Map<String, AppData> data = contentExporter.loadStructures(bs, baseRequest, state.type, c.getCollectionId(),
					toLoad);

			Iterator<ItemChangeReference> it = changes.items.iterator();
			while (it.hasNext()) {
				ItemChangeReference icr = it.next();
				if (data.containsKey(icr.getServerId().itemId)) {
					if (!icr.getData().isPresent()) {
						AppData appData = data.get(icr.getServerId().itemId);
						appData.body = VertxLazyLoader.wrap(appData.body);
						icr.setData(appData);
					}
				} else {
					// BM-11979
					// only remove add/change items with no data
					if (icr.getChangeType() == ChangeType.ADD || icr.getChangeType() == ChangeType.CHANGE) {
						logger.info("item {} has no data, remove it from changes", icr.getServerId());
						it.remove();
					}
				}
			}
		}

		return changes;
	}

	private BodyOptions asBaseRequest(CollectionSyncRequest c) {
		BodyOptions ret = null;
		if (c.options != null && c.options.bodyOptions != null) {
			ret = c.options.bodyOptions;
		} else {
			ret = new BodyOptions();
		}
		return ret;
	}

	private Changes applyWindowSize(CollectionSyncRequest c, Changes changes, BackendSession bs) {

		Queue<ItemChangeReference> toAdd = bs.getUnSynchronizedItemChange(c.getCollectionId());
		int inChanges = changes.items.size();
		int window = c.getWindowSize();
		if (inChanges > window) {
			Iterator<ItemChangeReference> it = changes.items.iterator();
			for (int i = 0; i < window; i++) {
				it.next();
			}
			while (it.hasNext()) {
				ItemChangeReference change = it.next();
				it.remove();
				toAdd.add(change);
			}
		} else {
			ItemChangeReference item = null;
			while (changes.items.size() < window && (item = toAdd.poll()) != null) {
				changes.items.add(item);
			}
		}
		int pending = toAdd.size();
		if (pending > 0) {
			Requests.tag(bs.getRequest(), "moreAvail", Integer.toString(pending));
		}

		logger.info("WindowSize is {}. Send {} changes. {} change(s) will be sent later", window, changes.items.size(),
				pending);

		return changes;
	}

	private ServerResponse clientFetch(BackendSession bs, CollectionSyncRequest c, IContentsExporter cex,
			ItemDataType dataType, CollectionItem item) throws ActiveSyncException {

		ItemChangeReference icr = new ItemChangeReference(dataType);
		icr.setServerId(item);

		ServerResponse sr = new ServerResponse();
		sr.item = item;
		BodyOptions bodyOptions = asBaseRequest(c);
		AppData fetched = cex.loadStructure(bs, bodyOptions, icr);
		if (fetched.body != null) {
			fetched.body = VertxLazyLoader.wrap(fetched.body);
		}
		c.fetched += 1;
		sr.fetch = Optional.of(fetched);
		sr.ackStatus = SyncStatus.OK;
		sr.operation = Operation.Fetch;
		return sr;
	}

	/**
	 * @param bs
	 * @param col
	 * @param dataType
	 * @return
	 * @throws ActiveSyncException
	 */
	private List<CollectionSyncResponse.ServerResponse> executeClientCommands(BackendSession bs,
			CollectionSyncRequest col, ItemDataType dataType) throws ActiveSyncException {

		if ("SMS".equals(col.getDataClass())) {
			return new ArrayList<>();
		}

		int size = col.getChangedItems().size() + col.getCreatedItems().size() + col.getFetchIds().size()
				+ col.getDeletedIds().size();

		logger.debug("[{}] {} changes requested by client", bs.getLoginAtDomain(), size);

		List<ServerResponse> ret = new ArrayList<>(size);

		IContentsImporter importer = backend.getContentsImporter(bs);

		if (!col.getDeletedIds().isEmpty()) {
			// COAX-261: do not send Delete Response (Exchange style)
			clientDelete(bs, col, importer, dataType, col.getDeletedIds());
		}

		StateMachine sm = new StateMachine(Backends.internalStorage());
		SyncState syncState = sm.getSyncState(bs, col.getCollectionId(), col.getSyncKey());

		if (col.getChangedItems().size() > 0) {
			for (Element e : col.getChangedItems()) {
				ret.add(clientChange(bs, col, importer, dataType, e, syncState));
			}
		}

		if (col.getCreatedItems().size() > 0) {
			for (Element e : col.getCreatedItems()) {
				ret.add(clientCreate(bs, col, importer, dataType, e, syncState));
			}
		}
		if (col.getFetchIds().size() > 0) {
			IContentsExporter cex = backend.getContentsExporter(bs);
			for (CollectionItem item : col.getFetchIds()) {
				ret.add(clientFetch(bs, col, cex, dataType, item));
			}
		}

		return ret;

	}

	/**
	 * Handles changes requested by mobile device
	 * 
	 * @param bs
	 * @param collection
	 * @param importer
	 * @param items
	 * @throws ActiveSyncException
	 */
	private CollectionSyncResponse.ServerResponse clientChange(BackendSession bs, CollectionSyncRequest collection,
			IContentsImporter importer, ItemDataType type, Element modification, SyncState syncState) {
		Integer collectionId = collection.getCollectionId();
		IDataDecoder dd = decoders.get(type);
		String serverId = DOMUtils.getElementText(modification, "ServerId");
		Element syncData = DOMUtils.getUniqueElement(modification, "ApplicationData");
		IApplicationData appData = dd.decode(bs, syncData);

		try {
			importer.importMessageChange(bs, collectionId, type, Optional.of(serverId), appData,
					collection.options.conflictPolicy, syncState);
			ServerResponse sr = new ServerResponse();
			sr.operation = Operation.Change;
			sr.item = CollectionItem.of(serverId);
			sr.ackStatus = SyncStatus.OK;
			return sr;
		} catch (ActiveSyncException e) {
			ServerResponse sr = new ServerResponse();
			sr.operation = Operation.Change;
			sr.item = CollectionItem.of(serverId);
			sr.ackStatus = SyncStatus.CONFLICT;
			return sr;
		}
	}

	/**
	 * Handles deletions requested by mobile device
	 * 
	 * @param bs
	 * @param collection
	 * @param importer
	 * @param items
	 * @return
	 * @throws ActiveSyncException
	 */
	private void clientDelete(BackendSession bs, CollectionSyncRequest collection, IContentsImporter importer,
			ItemDataType dataClass, Collection<CollectionItem> items) {
		try {
			importer.importMessageDeletion(bs, dataClass, items, collection.isDeletesAsMoves());
		} catch (ActiveSyncException e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * Handles modifications requested by mobile device
	 * 
	 * @param bs
	 * @param collection
	 * @param importer
	 * @param modification
	 * @param processedClientIds
	 */
	private ServerResponse clientCreate(BackendSession bs, CollectionSyncRequest collection, IContentsImporter importer,
			ItemDataType dataClass, Element modification, SyncState syncState) {
		Integer collectionId = collection.getCollectionId();
		String clientId = DOMUtils.getElementText(modification, "ClientId");

		Element syncData = DOMUtils.getUniqueElement(modification, "ApplicationData");
		IDataDecoder dd = decoders.get(dataClass);
		logger.info("[{}] processing Add (cli: {})", bs.getLoginAtDomain(), clientId);
		IApplicationData data = dd.decode(bs, syncData);
		HashMap<String, IApplicationData> d = new HashMap<String, IApplicationData>();
		d.put(null, data);
		try {
			CollectionItem bmId = importer.importMessageChange(bs, collectionId, dataClass, Optional.<String>empty(),
					data, collection.options.conflictPolicy, syncState);
			ServerResponse sr = new ServerResponse();
			sr.clientId = clientId;
			sr.ackStatus = SyncStatus.OK;
			sr.item = bmId;
			sr.operation = Operation.Add;
			return sr;
		} catch (ActiveSyncException e) {
			ServerResponse sr = new ServerResponse();
			sr.clientId = clientId;
			sr.item = CollectionItem.of(collectionId, UUID.randomUUID().toString());
			sr.ackStatus = SyncStatus.SERVER_ERROR;
			return sr;
		}
	}

	/**
	 * Get server-side changes if necessary
	 * 
	 * @param bs
	 * @param c
	 * @param clientAddedServerIds
	 * @return
	 */
	private CollectionChanges serverChanges(BackendSession bs, CollectionSyncRequest c,
			List<String> clientAddedServerIds) {
		CollectionChanges cc = new CollectionChanges();
		cc.syncKey = c.getSyncKey();
		StateMachine sm = new StateMachine(Backends.internalStorage());
		try {
			String syncKey = c.getSyncKey();

			SyncState st = sm.getSyncState(bs, c.getCollectionId(), syncKey);

			if (st == null) {
				logger.error("Send Invalid SyncKey to device {}. key: {}", bs.getDevId(), syncKey);
				cc.status = SyncStatus.INVALID_SYNC_KEY;
				c.forceResponse = true;
			} else {
				Changes changes = null;
				if (shouldFetchChanges(c)) {
					changes = doSync(bs, c, st, clientAddedServerIds);
					List<ServerChange> cmds = new ArrayList<>(changes.items.size());
					cc.commands = cmds;
					int remaining = bs.getUnSynchronizedItemChange(c.getCollectionId()).size();
					if (remaining > 0) {
						cc.moreAvailable = true;
						logger.debug("**** {} MORE ITEMS AVAILABLE ****", remaining);
					}
					for (ItemChangeReference cr : changes.items) {
						ServerChange srvChange = asServerChange(cr);
						cmds.add(srvChange);
					}
					cc.syncKey = sm.generateSyncKey(st.type, changes.version);
				} else {
					c.forceResponse = true;
					cc.syncKey = sm.generateSyncKey(st.type, st.version);

				}
				bs.addLastClientSyncState(c.getCollectionId(), st);
			}
		} catch (CollectionNotFoundException e) {
			logger.error(e.getMessage(), e);
			// FIXME validate connections earlier
		} catch (ActiveSyncException e) {
			logger.error(e.getMessage(), e);
		}
		return cc;
	}

	private ServerChange asServerChange(ItemChangeReference cr) {
		ServerChange srvChange = new ServerChange();
		srvChange.data = cr.getData();
		srvChange.item = cr.getServerId();
		switch (cr.getChangeType()) {
		case CHANGE:
			srvChange.type = ServerChange.ChangeType.Change;
			break;
		case DELETE:
			srvChange.type = ServerChange.ChangeType.Delete;
			break;
		case SOFTDELETE:
			srvChange.type = ServerChange.ChangeType.SoftDelete;
			break;
		default:
		case ADD:
			srvChange.type = ServerChange.ChangeType.Add;
			break;

		}
		return srvChange;
	}

	private void sendError(Handler<SyncResponse> respHandler, SyncStatus status) {
		SyncResponse sr = new SyncResponse();
		sr.status = status;
		respHandler.handle(sr);
	}

	private void sendLimitError(Handler<SyncResponse> respHandler, int limit) {
		SyncResponse sr = new SyncResponse();
		sr.status = SyncStatus.WAIT_INTERVAL_OUT_OF_RANGE;
		sr.limit = limit;
		respHandler.handle(sr);
	}

}
