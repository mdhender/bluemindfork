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
package net.bluemind.eas.command.ping;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.dto.EasBusEndpoints;
import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.OptionalParams;
import net.bluemind.eas.dto.ping.PingRequest;
import net.bluemind.eas.dto.ping.PingRequest.Folders.Folder;
import net.bluemind.eas.dto.ping.PingResponse;
import net.bluemind.eas.dto.ping.PingResponse.Status;
import net.bluemind.eas.dto.sync.CollectionId;
import net.bluemind.eas.dto.sync.CollectionSyncRequest;
import net.bluemind.eas.impl.Backends;
import net.bluemind.eas.impl.Responder;
import net.bluemind.eas.protocol.IEasProtocol;
import net.bluemind.eas.serdes.ping.PingRequestParser;
import net.bluemind.eas.serdes.ping.PingResponseFormatter;
import net.bluemind.eas.store.ISyncStorage;
import net.bluemind.eas.utils.EasLogUser;
import net.bluemind.eas.wbxml.builder.WbxmlResponseBuilder;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.vertx.common.request.Requests;

public class PingProtocol implements IEasProtocol<PingRequest, PingResponse> {
	private static final Logger logger = LoggerFactory.getLogger(PingProtocol.class);
	private static HeartbeatSync heartbeatSync;
	private final ISyncStorage store;
	private static final Cache<String, Integer> heartbeat = Caffeine.newBuilder().recordStats().build();

	static {
		heartbeatSync = new HeartbeatSync();
		heartbeatSync.start(heartbeat);
	}

	public static class CacheRegistration implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register(PingProtocol.class, heartbeat);
		}
	}

	public PingProtocol() {
		store = Backends.internalStorage();
	}

	@Override
	public void parse(BackendSession bs, OptionalParams optParams, Document doc, IPreviousRequestsKnowledge past,
			Handler<PingRequest> parserResultHandler) {
		if (logger.isDebugEnabled()) {
			EasLogUser.logDebugAsUser(bs.getLoginAtDomain(), logger, "******** Parsing *******");
		}

		PingRequestParser parser = new PingRequestParser();
		PingRequest parsed = parser.parse(optParams, doc, past, bs.getLoginAtDomain());
		parserResultHandler.handle(parsed);
	}

	@Override
	public void execute(BackendSession bs, PingRequest query, Handler<PingResponse> responseHandler) {
		if (logger.isDebugEnabled()) {
			EasLogUser.logDebugAsUser(bs.getLoginAtDomain(), logger, "******** Executing *******");
		}

		PingResponse response = new PingResponse();

		long intervalSeconds = getLastHeartbeat(bs);

		if (query == null) {
			if (bs.getLastMonitored() == null || bs.getLastMonitored().isEmpty()) {
				EasLogUser.logWarnAsUser(bs.getLoginAtDomain(), logger,
						"[{}][{}] Don't know what to monitor, interval: {} toMonitor: {}. Send status 3 MissingParameter",
						bs.getLoginAtDomain(), bs.getDevId(), intervalSeconds, bs.getLastMonitored());
				response.status = Status.MISSING_PARAMETER;
				responseHandler.handle(response);
				return;
			}
			EasLogUser.logInfoAsUser(bs.getLoginAtDomain(), logger,
					"[{}][{}] Empty Ping, reusing cached heartbeat & monitored folders ({})", bs.getLoginAtDomain(),
					bs.getDevId(), bs.getLastMonitored().size());
		} else {
			Set<CollectionSyncRequest> toMonitor = new HashSet<>();
			for (Folder folder : query.folders.folders) {
				try {
					CollectionSyncRequest sc = new CollectionSyncRequest();
					sc.setDataClass(folder.clazz.name());
					sc.setCollectionId(CollectionId.of(folder.id));
					toMonitor.add(sc);
				} catch (NumberFormatException nfe) {
					// HTC ONE X sends "InvalidTaskID" as folder.id
					EasLogUser.logWarnAsUser(bs.getLoginAtDomain(), logger, "[{}][{}] Invalid collectionId {}",
							bs.getLoginAtDomain(), bs.getDevId(), folder.id);
				}

			}
			if (!query.folders.folders.isEmpty()) {
				bs.setLastMonitored(toMonitor);
			}

			// when push list is empty, send MissingParameter
			if (bs.getLastMonitored() == null || bs.getLastMonitored().isEmpty()) {
				EasLogUser.logWarnAsUser(bs.getLoginAtDomain(), logger,
						"[{}][{}]  Nothing to monitor. Send status 3 MissingParameter", bs.getLoginAtDomain(),
						bs.getDevId());
				response.status = Status.MISSING_PARAMETER;
				responseHandler.handle(response);
				return;
			}

			// check heartbeat value is in configured interval
			if (query.heartbeatInterval != null) {
				intervalSeconds = query.heartbeatInterval;
			}

			int maxInterval = getInterval("eas_max_heartbeat", 1130);
			if (intervalSeconds > maxInterval) {
				EasLogUser.logWarnAsUser(bs.getLoginAtDomain(), logger,
						"[{}][{}] Send Heartbeat error: intervalSeconds {} > maxInterval {}", bs.getLoginAtDomain(),
						bs.getDevId(), intervalSeconds, maxInterval);
				response.status = Status.INVALID_HEARTBEAT_INTERVAL;
				response.heartbeatInterval = maxInterval;
				responseHandler.handle(response);
				return;
			}

			int minInterval = getInterval("eas_min_heartbeat", 120);
			if (intervalSeconds < minInterval) {
				EasLogUser.logWarnAsUser(bs.getLoginAtDomain(), logger,
						"[{}][{}] Send Heartbeat error: intervalSeconds {} < minInterval {}", bs.getLoginAtDomain(),
						bs.getDevId(), intervalSeconds, minInterval);
				response.status = Status.INVALID_HEARTBEAT_INTERVAL;
				response.heartbeatInterval = minInterval;
				responseHandler.handle(response);
				return;
			}

			if (intervalSeconds != bs.getHeartbeart()) {
				bs.setHeartbeart(intervalSeconds);
				store.updateLastHearbeat(bs.getDeviceId(), intervalSeconds);
			}
		}

		if (intervalSeconds > 0 && bs.getLastMonitored() != null) {
			Requests.tagAsync(bs.getRequest());
			Requests.tag(bs.getRequest(), "timeout", intervalSeconds + "s");

			final List<MessageConsumer<JsonObject>> consumers = new LinkedList<>();
			final AtomicBoolean responseSent = new AtomicBoolean();
			long noChangesTimer = VertxPlatform.getVertx().setTimer(TimeUnit.SECONDS.toMillis(intervalSeconds), tid -> {
				// noChanges
				if (responseSent.getAndSet(true)) {
					return;
				}
				consumers.forEach(MessageConsumer::unregister);
				responseHandler.handle(noChangesResponse());
			});

			for (CollectionSyncRequest sc : bs.getLastMonitored()) {
				MessageConsumer<JsonObject> cons = VertxPlatform.eventBus()
						.consumer("eas.collection." + sc.getCollectionId().getFolderId());
				consumers.add(cons);
				Handler<Message<JsonObject>> colChangeHandler = (Message<JsonObject> msg) -> {
					// syncRequired
					if (responseSent.getAndSet(true)) {
						return;
					}
					consumers.forEach(MessageConsumer::unregister);
					VertxPlatform.getVertx().cancelTimer(noChangesTimer);
					PingResponse pr = new PingResponse();
					pr.status = Status.CHANGES_OCCURRED;
					pr.folders = new PingResponse.Folders();
					pr.folders.folders.add(sc.getCollectionId().getValue());
					responseHandler.handle(pr);
				};
				cons.handler(colChangeHandler);
			}
			MessageConsumer<JsonObject> hierCons = VertxPlatform.eventBus()
					.consumer("eas.hierarchy." + bs.getUser().getUid());
			consumers.add(hierCons);
			Handler<Message<JsonObject>> hierChangeHandler = (Message<JsonObject> msg) -> {
				// folderSyncRequired
				if (responseSent.getAndSet(true)) {
					return;
				}
				consumers.forEach(MessageConsumer::unregister);
				VertxPlatform.getVertx().cancelTimer(noChangesTimer);
				PingResponse pr = new PingResponse();
				pr.status = Status.FOLDER_SYNC_REQUIRED;
				responseHandler.handle(pr);
			};
			hierCons.handler(hierChangeHandler);

			MessageConsumer<JsonObject> pushKiller = VertxPlatform.eventBus()
					.consumer(EasBusEndpoints.PUSH_KILLER + "." + bs.getUniqueIdentifier());
			consumers.add(pushKiller);
			pushKiller.handler(msg -> {
				if (responseSent.getAndSet(true)) {
					return;
				}
				consumers.forEach(MessageConsumer::unregister);
				VertxPlatform.getVertx().cancelTimer(noChangesTimer);
				responseHandler.handle(noChangesResponse());
				msg.reply("ok");
			});

		} else {
			EasLogUser.logWarnAsUser(bs.getLoginAtDomain(), logger,
					"[{}][{}] Don't know what to monitor, interval is null. Send status 3 MissingParameter",
					bs.getLoginAtDomain(), bs.getDevId());
			response.status = Status.MISSING_PARAMETER;
			responseHandler.handle(response);
		}

	}

	private PingResponse noChangesResponse() {
		PingResponse pr = new PingResponse();
		pr.status = Status.NO_CHANGES;
		return pr;
	}

	private long getLastHeartbeat(BackendSession bs) {
		Long hb = bs.getHeartbeart();
		if (hb == null) {
			hb = store.findLastHeartbeat(bs.getDeviceId());
			bs.setHeartbeart(hb);
		}
		return hb;
	}

	private int getInterval(String k, int defaultValue) {
		Integer ret = heartbeat.getIfPresent(k);
		if (ret == null) {
			ret = defaultValue;
			String val = store.getSystemConf(k);
			if (val != null) {
				try {
					ret = Integer.parseInt(val);
				} catch (NumberFormatException nfe) {
					logger.error("Invalid {} value {} ", k, val);
				}
			}
			heartbeat.put(k, ret);
		}

		return ret;
	}

	@Override
	public void write(BackendSession bs, Responder responder, PingResponse response, final Handler<Void> completion) {
		PingResponseFormatter formatter = new PingResponseFormatter();
		WbxmlResponseBuilder builder = new WbxmlResponseBuilder(bs.getProtocolVersion(), bs.getLoginAtDomain(),
				responder.asOutput());
		formatter.format(builder, bs.getProtocolVersion(), response, data -> completion.handle(data));
	}

	@Override
	public String address() {
		return "eas.protocol.ping";
	}

}
