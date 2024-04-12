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
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.Changes;
import net.bluemind.eas.backend.HierarchyNode;
import net.bluemind.eas.backend.IApplicationData;
import net.bluemind.eas.backend.IBackend;
import net.bluemind.eas.backend.IContentsExporter;
import net.bluemind.eas.backend.IContentsImporter;
import net.bluemind.eas.backend.ItemChangeReference;
import net.bluemind.eas.command.folder.sync.FolderSyncProtocol;
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
import net.bluemind.eas.exception.ObjectNotFoundException;
import net.bluemind.eas.impl.Backends;
import net.bluemind.eas.impl.Responder;
import net.bluemind.eas.impl.vertx.VertxLazyLoader;
import net.bluemind.eas.protocol.IEasProtocol;
import net.bluemind.eas.protocol.ProtocolCircuitBreaker;
import net.bluemind.eas.serdes.DateFormat;
import net.bluemind.eas.serdes.IResponseBuilder;
import net.bluemind.eas.serdes.sync.SyncRequestParser;
import net.bluemind.eas.serdes.sync.SyncResponseFormatter;
import net.bluemind.eas.state.StateMachine;
import net.bluemind.eas.utils.DOMUtils;
import net.bluemind.eas.utils.EasLogUser;
import net.bluemind.eas.wbxml.builder.WbxmlResponseBuilder;
import net.bluemind.lib.vertx.VertxPlatform;
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
		this.decoders = new EnumMap<>(ItemDataType.class);
		decoders.put(ItemDataType.CONTACTS, new ContactDecoder());
		decoders.put(ItemDataType.CALENDAR, new CalendarDecoder());
		decoders.put(ItemDataType.EMAIL, new EmailDecoder());
		decoders.put(ItemDataType.TASKS, new TaskDecoder());
	}

	@Override
	public void parse(BackendSession bs, OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past,
			Handler<SyncRequest> parserResultHandler) {
		SyncRequest sr = new SyncRequestParser().parse(optParams, doc, past, bs.getLoginAtDomain());
		parserResultHandler.handle(sr);
	}

	private boolean shouldFetchChanges(CollectionSyncRequest sc) {
		Boolean inDOM = sc.isGetChanges();
		if (inDOM == null) {
			return !"0".equals(sc.getSyncKey());
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
					EasLogUser.logErrorAsUser(bs.getLoginAtDomain(), logger, "Invalid heartbeat value: {}", maxString);
				}
			}
			if (sr.heartbeatInterval > maxInterval) {
				EasLogUser.logWarnAsUser(bs.getLoginAtDomain(), logger, "Invalid HeartbeatInterval {} > {}",
						sr.heartbeatInterval, maxInterval);
				sendLimitError(responseHandler, maxInterval);
				return;
			}
		}

		if (sr.waitIntervalSeconds != null && sr.waitIntervalSeconds > 59 * 60) {
			sendLimitError(responseHandler, 59);
			return;
		}

		// tom: when push is enabled, can we have client changes ?
		// assume NO

		if (sr.waitIntervalSeconds == null && sr.heartbeatInterval == null) {
			JsonObject jso = new JsonObject();
			for (CollectionSyncRequest sc : sr.collections) {
				jso.put(sc.getCollectionId().getValue(), bs.getDeviceId().getInternalId());
			}
			EventBus eb = VertxPlatform.eventBus();
			eb.request(EasBusEndpoints.PUSH_KILLER + "." + bs.getUniqueIdentifier(), jso,
					(AsyncResult<Message<Void>> event) -> {
						EasLogUser.logDebugAsUser(bs.getLoginAtDomain(), logger, "Push stopped for {}",
								bs.getUniqueIdentifier());
						VertxPlatform.getVertx().executeBlocking(() -> {
							executeSync(bs, sr, responseHandler);
							return null;
						}, false);
					});
		} else {
			EasLogUser.logInfoAsUser(bs.getLoginAtDomain(), logger,
					"Sync push mode. user: {}, device: {}, collections size: {}", bs.getLoginAtDomain(), bs.getDevId(),
					bs.getLastMonitored().size());
			bs.setLastWaitSeconds(sr.waitIntervalSeconds);
			bs.setHeartbeart(Long.valueOf(sr.heartbeatInterval));
			Requests.tagAsync(bs.getRequest());
			Requests.tag(bs.getRequest(), "timeout", sr.waitIntervalSeconds + "s");

			prepareAsyncResponse(bs, sr, responseHandler);

		}

	}

	private void prepareAsyncResponse(final BackendSession bs, final SyncRequest sr,
			final Handler<SyncResponse> responseHandler) {
		final Set<CollectionSyncRequest> collections = new LinkedHashSet<>(bs.getLastMonitored());
		final List<MessageConsumer<JsonObject>> consumers = new LinkedList<>();
		final AtomicBoolean responseSent = new AtomicBoolean();

		Integer delayInSec = sr.waitIntervalSeconds != null ? sr.waitIntervalSeconds : sr.heartbeatInterval;
		long noChangesTimer = VertxPlatform.getVertx().setTimer(TimeUnit.SECONDS.toMillis(delayInSec), tid -> {
			if (responseSent.getAndSet(true)) {
				return;
			}

			// noChanges
			consumers.forEach(MessageConsumer::unregister);
			responseHandler.handle(noChangesResponse(collections));
		});

		for (CollectionSyncRequest colId : collections) {
			MessageConsumer<JsonObject> cons = VertxPlatform.eventBus()
					.consumer("eas.collection." + colId.getCollectionId().getFolderId());
			consumers.add(cons);
			Handler<Message<JsonObject>> colChangeHandler = (Message<JsonObject> msg) -> {
				if (responseSent.getAndSet(true)) {
					return;
				}
				// syncRequired
				consumers.forEach(MessageConsumer::unregister);
				VertxPlatform.getVertx().cancelTimer(noChangesTimer);

				SyncResponse syncResponse = new SyncResponse();
				for (CollectionSyncRequest sc : collections) {
					CollectionSyncResponse csr = new CollectionSyncResponse();
					csr.collectionId = sc.getCollectionId().getValue();
					CollectionChanges serverChanges = serverChanges(bs, sc, new ArrayList<>());
					csr.commands = serverChanges.commands;
					csr.status = serverChanges.status;
					csr.syncKey = serverChanges.syncKey;
					csr.moreAvailable = serverChanges.moreAvailable;
					syncResponse.collections.add(csr);
				}
				responseHandler.handle(syncResponse);

			};
			cons.handler(colChangeHandler);
		}

		MessageConsumer<JsonObject> pushKiller = VertxPlatform.eventBus()
				.consumer(EasBusEndpoints.PUSH_KILLER + "." + bs.getUniqueIdentifier());
		consumers.add(pushKiller);
		pushKiller.handler(msg -> {
			if (responseSent.getAndSet(true)) {
				return;
			}
			consumers.forEach(MessageConsumer::unregister);
			VertxPlatform.getVertx().cancelTimer(noChangesTimer);
			responseHandler.handle(noChangesResponse(collections));
			msg.reply("ok");
		});
	}

	private SyncResponse noChangesResponse(final Set<CollectionSyncRequest> collections) {
		SyncResponse syncResponse = new SyncResponse();
		for (CollectionSyncRequest sc : collections) {
			CollectionSyncResponse csr = new CollectionSyncResponse();
			csr.collectionId = sc.getCollectionId().getValue();
			csr.status = SyncStatus.OK;
			csr.syncKey = sc.getSyncKey();
			syncResponse.collections.add(csr);
		}
		return syncResponse;
	}

	private void executeSync(BackendSession bs, SyncRequest sr, Handler<SyncResponse> responseHandler) {
		SyncResponse syncResponse = new SyncResponse();
		int syncErrors = 0;

		for (CollectionSyncRequest sc : sr.collections) {
			CollectionSyncResponse csr = new CollectionSyncResponse();
			csr.collectionId = sc.getCollectionId().getValue();

			try {
				// ensure the collectionExists
				HierarchyNode f = Backends.internalStorage().getHierarchyNode(bs, sc.getCollectionId());

				ItemDataType dataClass = ItemDataType.getValue(f.containerType);

				List<ServerResponse> clientChangeResults = executeClientCommands(bs, sc, dataClass);

				List<String> clientAddedServerIds = new ArrayList<>(clientChangeResults.size());
				List<ServerResponse> clientConflictedServerIds = new ArrayList<>(clientChangeResults.size());

				List<ServerResponse> clientErrorServerIds = new ArrayList<>(clientChangeResults.size());

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

				List<Long> commands = serverChanges.commands.stream().map(cmd -> cmd.item.itemId)
						.collect(Collectors.toList());
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
					if (sse.operation == Operation.CHANGE) {
						conflicted.type = ServerChange.ChangeType.CHANGE;
					} else if (sse.operation == Operation.DELETE) {
						conflicted.type = ServerChange.ChangeType.ADD;
					}
					conflicted.item = sse.item;
					csr.commands.add(conflicted);
				}

				for (ServerResponse sse : clientErrorServerIds) {
					sse.ackStatus = SyncStatus.OK;
					sse.operation = Operation.ADD;
					ServerChange err = new ServerChange();
					err.item = sse.item;
					err.type = ServerChange.ChangeType.DELETE;
					err.data = Optional.empty();
					csr.commands.add(err);
				}
				csr.status = serverChanges.status;
				csr.syncKey = serverChanges.syncKey;
				csr.moreAvailable = serverChanges.moreAvailable;
			} catch (CollectionNotFoundException cnf) {
				sr.invalidCollections.add(csr.collectionId);
			} catch (Exception e) {
				EasLogUser.logExceptionAsUser(bs.getLoginAtDomain(), e, logger);

				csr.syncKey = sc.getSyncKey();
				csr.status = SyncStatus.SERVER_ERROR;
				syncErrors++;
			}

			syncResponse.collections.add(csr);
		}

		sr.invalidCollections.forEach(collectionId -> {
			CollectionSyncResponse csr = new CollectionSyncResponse();
			csr.collectionId = collectionId;
			csr.status = SyncStatus.OBJECT_NOT_FOUND;
			csr.commands = Collections.emptyList();
			csr.responses = Collections.emptyList();
			syncResponse.collections.add(csr);
		});

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

		Callback<Void> cb = dom -> {
			Backends.internalStorage().updateLastSync(bs);
			completion.handle(null);
		};
		IResponseBuilder builder = new WbxmlResponseBuilder(bs.getProtocolVersion(), bs.getLoginAtDomain(),
				responder.asOutput());
		SyncResponseFormatter srf = new SyncResponseFormatter();
		srf.format(builder, bs.getProtocolVersion(), response, cb);
	}

	@Override
	public String address() {
		return "eas.protocol.sync";
	}

	/**
	 * Returns the list of items for which we sent an 'Add' command.
	 * 
	 */
	private Changes doSync(BackendSession bs, CollectionSyncRequest c, SyncState state,
			List<String> clientAddedServerIds) throws ActiveSyncException {

		Changes changes = new Changes();

		IContentsExporter contentExporter = backend.getContentsExporter(bs);

		if (bs.getUnSynchronizedItemChange(c.getCollectionId()).isEmpty()) {
			changes = contentExporter.getChanged(bs, state, c.options, c.getCollectionId());
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

		List<Long> toLoad = changes.items.stream().filter(
				change -> (change.getChangeType() == ChangeType.ADD || change.getChangeType() == ChangeType.CHANGE))
				.map(i -> i.getServerId().itemId).collect(Collectors.toList());

		if (!toLoad.isEmpty()) {
			Map<Long, AppData> data = contentExporter.loadStructures(bs, baseRequest, state.type, c.getCollectionId(),
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
						EasLogUser.logInfoAsUser(bs.getLoginAtDomain(), logger,
								"item {} has no data, remove it from changes", icr.getServerId());
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

		EasLogUser.logDebugAsUser(bs.getLoginAtDomain(), logger,
				"WindowSize is {}. Send {} changes. {} change(s) will be sent later", window, changes.items.size(),
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
		sr.operation = Operation.FETCH;
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

		int size = col.getChangedItems().size() + col.getCreatedItems().size() + col.getFetchIds().size()
				+ col.getDeletedIds().size();

		EasLogUser.logDebugAsUser(bs.getLoginAtDomain(), logger, "[{}] {} changes requested by client",
				bs.getLoginAtDomain(), size);

		List<ServerResponse> ret = new ArrayList<>(size);

		IContentsImporter importer = backend.getContentsImporter(bs);

		StateMachine sm = new StateMachine(Backends.internalStorage());
		SyncState syncState = sm.getSyncState(bs, col.getCollectionId(), col.getSyncKey());

		if (!"SMS".equals(col.getDataClass())) {
			if (!col.getDeletedIds().isEmpty()) {
				// COAX-261: do not send Delete Response (Exchange style)
				clientDelete(bs, col, importer, dataType, col.getDeletedIds());
			}
			for (Element e : col.getCreatedItems()) {
				ret.add(clientCreate(bs, col, importer, dataType, e, syncState));
			}
			IContentsExporter cex = backend.getContentsExporter(bs);
			for (CollectionItem item : col.getFetchIds()) {
				ret.add(clientFetch(bs, col, cex, dataType, item));
			}
		}

		for (Element e : col.getChangedItems()) {
			ret.add(clientChange(bs, col, importer, dataType, e, syncState));
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
		IDataDecoder dd = decoders.get(type);
		Element syncData = DOMUtils.getUniqueElement(modification, "ApplicationData");
		String serverId = DOMUtils.getElementText(modification, "ServerId");
		String instanceId = DOMUtils.getElementText(modification, "InstanceId");

		IApplicationData appData = null;

		Date recurid = null;
		if (instanceId != null) {
			recurid = DateFormat.parse(instanceId);
			appData = dd.decode(bs, syncData, Map.of("hasRecurId", Boolean.TRUE));
		} else {
			appData = dd.decode(bs, syncData);
		}
		if (type == ItemDataType.EMAIL && DOMUtils.getUniqueElement(modification, "Send") != null) {
			// MS-ASEMAIL 2.2.2.69 Send
			//
			// The Send element is an empty tag element, meaning it has no value or data
			// type. It is distinguished only by the presence or absence of the <Send/> tag.
			// The presence of the tag in a Sync command request indicates that the email is
			// to be sent; the absence of the tag indicates that the email is to be saved as
			// a draft.
			try {
				importer.sendDraft(bs, serverId, appData);
				return clientChangeSuccess(serverId);
			} catch (ActiveSyncException e) {
				return clientChangeError(bs, serverId, e);
			}
		}

		try {
			importer.importMessageChange(bs, collection.getCollectionId(), type, Optional.of(serverId),
					Optional.ofNullable(recurid), appData, collection.options.conflictPolicy, syncState);
			return clientChangeSuccess(serverId);
		} catch (ObjectNotFoundException e) {
			return clientChangedObjectNotFoundError(bs, serverId, e);
		} catch (ActiveSyncException e) {
			return clientChangeError(bs, serverId, e);
		}
	}

	private CollectionSyncResponse.ServerResponse clientChangeSuccess(String serverId) {
		ServerResponse sr = new ServerResponse();
		sr.operation = Operation.CHANGE;
		sr.item = CollectionItem.of(serverId);
		sr.ackStatus = SyncStatus.OK;
		return sr;
	}

	private CollectionSyncResponse.ServerResponse clientChangeError(BackendSession bs, String serverId, Exception e) {
		EasLogUser.logExceptionAsUser(bs.getLoginAtDomain(), e, logger);
		ServerResponse sr = new ServerResponse();
		sr.operation = Operation.CHANGE;
		sr.item = CollectionItem.of(serverId);
		sr.ackStatus = SyncStatus.CONFLICT;
		return sr;
	}

	private CollectionSyncResponse.ServerResponse clientChangedObjectNotFoundError(BackendSession bs, String serverId,
			Exception e) {
		EasLogUser.logExceptionAsUser(bs.getLoginAtDomain(), e, logger);
		ServerResponse sr = new ServerResponse();
		sr.operation = Operation.CHANGE;
		sr.item = CollectionItem.of(serverId);
		sr.ackStatus = SyncStatus.OBJECT_NOT_FOUND;
		return sr;
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
			EasLogUser.logExceptionAsUser(bs.getLoginAtDomain(), e, logger);
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
		String clientId = DOMUtils.getElementText(modification, "ClientId");

		Element syncData = DOMUtils.getUniqueElement(modification, "ApplicationData");
		IDataDecoder decoder = decoders.get(dataClass);
		EasLogUser.logInfoAsUser(bs.getLoginAtDomain(), logger, "[{}] processing Add (dataClass: {}, cli: {})",
				bs.getLoginAtDomain(), dataClass, clientId);
		IApplicationData data = decoder.decode(bs, syncData);
		HashMap<String, IApplicationData> d = new HashMap<>();
		d.put(null, data);
		try {
			CollectionItem bmId = importer.importMessageChange(bs, collection.getCollectionId(), dataClass,
					Optional.<String>empty(), Optional.empty(), data, collection.options.conflictPolicy, syncState);
			ServerResponse sr = new ServerResponse();
			sr.clientId = clientId;
			sr.ackStatus = SyncStatus.OK;
			sr.item = bmId;
			sr.operation = Operation.ADD;
			return sr;
		} catch (ActiveSyncException e) {
			EasLogUser.logExceptionAsUser(bs.getLoginAtDomain(), e, logger);
			ServerResponse sr = new ServerResponse();
			sr.clientId = clientId;
			sr.item = CollectionItem.of(collection.getCollectionId(), System.currentTimeMillis());
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
				EasLogUser.logWarnAsUser(bs.getLoginAtDomain(), logger,
						"Send status 3 Invalid SyncKey to device {}. key: {}", bs.getDevId(), syncKey);
				cc.status = SyncStatus.INVALID_SYNC_KEY;
				c.forceResponse = true;
			} else {
				if (needsGlobalsync(bs, StateMachine.extractTimestamp(bs.getLoginAtDomain(), syncKey))) {
					st.version = 0;
				}

				Changes changes = null;
				if (shouldFetchChanges(c)) {
					changes = doSync(bs, c, st, clientAddedServerIds);
					List<ServerChange> cmds = new ArrayList<>(changes.items.size());
					cc.commands = cmds;
					int remaining = bs.getUnSynchronizedItemChange(c.getCollectionId()).size();
					if (remaining > 0) {
						cc.moreAvailable = true;
						EasLogUser.logDebugAsUser(bs.getLoginAtDomain(), logger, "**** {} MORE ITEMS AVAILABLE ****",
								remaining);
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
				bs.addLastClientSyncState(c.getCollectionId().getValue(), st);
			}
		} catch (CollectionNotFoundException e) {
			EasLogUser.logExceptionAsUser(bs.getLoginAtDomain(), e, logger);
			// FIXME validate connections earlier
		} catch (ActiveSyncException e) {
			EasLogUser.logExceptionAsUser(bs.getLoginAtDomain(), e, logger);
		}
		return cc;
	}

	private boolean needsGlobalsync(BackendSession bs, long syncKeyTimestamp) {
		String deviceId = bs.getDevId();
		long reset = FolderSyncProtocol.getLastReset(deviceId);
		if (syncKeyTimestamp < reset) {
			EasLogUser.logInfoAsUser(bs.getLoginAtDomain(), logger, "Pending reset for {}, time: {}, sync key ts: {}",
					deviceId, reset, syncKeyTimestamp);
			return true;
		}
		return false;
	}

	private ServerChange asServerChange(ItemChangeReference cr) {
		ServerChange srvChange = new ServerChange();
		srvChange.data = cr.getData();
		srvChange.item = cr.getServerId();
		switch (cr.getChangeType()) {
		case CHANGE:
			srvChange.type = ServerChange.ChangeType.CHANGE;
			break;
		case DELETE:
			srvChange.type = ServerChange.ChangeType.DELETE;
			break;
		case SOFTDELETE:
			srvChange.type = ServerChange.ChangeType.SOFT_DELETE;
			break;
		default:
		case ADD:
			srvChange.type = ServerChange.ChangeType.ADD;
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
