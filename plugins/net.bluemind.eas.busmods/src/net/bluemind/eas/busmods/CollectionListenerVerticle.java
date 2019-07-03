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
package net.bluemind.eas.busmods;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import net.bluemind.eas.dto.EasBusEndpoints;
import net.bluemind.eas.dto.push.PushRegistrationRequest;
import net.bluemind.eas.dto.push.PushTrigger;
import net.bluemind.vertx.common.LocalJsonObject;

public class CollectionListenerVerticle extends BusModBase {
	private static final Logger logger = LoggerFactory.getLogger(CollectionListenerVerticle.class);

	private Handler<Message<LocalJsonObject<PushRegistrationRequest>>> registrationHandler;

	private static final class PushRegKey {
		public final String partnershipId;
		public final int collectionId;

		PushRegKey(String pid, int cid) {
			this.partnershipId = pid;
			this.collectionId = cid;
		}

	}

	private ConcurrentHashMap<PushRegKey, PushRegistrationRequest> keyToReplyAddress = new ConcurrentHashMap<>();
	private Multimap<Integer, PushRegKey> idxByCollectionId = Multimaps.newMultimap(
			new ConcurrentHashMap<Integer, Collection<PushRegKey>>(), () -> new CopyOnWriteArrayList<PushRegKey>());
	private Multimap<String, PushRegKey> idxByPartnershipId = Multimaps.newMultimap(
			new ConcurrentHashMap<String, Collection<PushRegKey>>(), () -> new CopyOnWriteArrayList<PushRegKey>());

	private Handler<Message<LocalJsonObject<PushTrigger>>> triggerHandler;

	public void start() {
		super.start();

		registrationHandler = new Handler<Message<LocalJsonObject<PushRegistrationRequest>>>() {

			@Override
			public void handle(Message<LocalJsonObject<PushRegistrationRequest>> event) {
				final PushRegistrationRequest pushReg = event.body().getValue();
				final List<PushRegKey> pushRegKeys = new ArrayList<>(pushReg.collectionIds.size());
				pushReg.replyAddress = event.replyAddress();
				logger.debug("Got registration: {}. Response goes to {}", pushReg, event.replyAddress());
				for (Integer collectionId : pushReg.collectionIds) {
					logger.debug("register for push: partnershipId {}, collectionId {}", pushReg.pushKey, collectionId);
					PushRegKey prk = new PushRegKey(pushReg.pushKey, collectionId);
					keyToReplyAddress.put(prk, pushReg);
					idxByCollectionId.put(prk.collectionId, prk);
					idxByPartnershipId.put(prk.partnershipId, prk);
					pushRegKeys.add(prk);
				}

				// register a pushRegKey for folderSync, userUid as key
				PushRegKey prk = new PushRegKey(pushReg.userUid, 0);
				keyToReplyAddress.put(prk, pushReg);
				pushRegKeys.add(prk);

				long timeOutId = vertx.setTimer(pushReg.timeoutMs, new Handler<Long>() {

					@Override
					public void handle(Long event) {
						logger.info("Cleaning up timed-out push registration for pid {}, {} collections.",
								pushReg.pushKey, pushReg.collectionIds.size());
						int removedCount = 0;
						for (PushRegKey prk : pushRegKeys) {
							PushRegistrationRequest removed = keyToReplyAddress.remove(prk);
							idxByCollectionId.remove(prk.collectionId, prk);
							idxByPartnershipId.remove(prk.partnershipId, prk);
							if (removed != null) {
								removedCount++;
							}
						}
						logger.info("Timeout has removed {} monitored collections.", removedCount);
					}
				});
				// for cancellation once triggered
				pushReg.expirationTimerId = timeOutId;
				logger.info("{} monitored items for push notification.", keyToReplyAddress.size());
			}

		};
		eb.registerHandler(EasBusEndpoints.PUSH_REGISTRATION, registrationHandler);

		triggerHandler = new Handler<Message<LocalJsonObject<PushTrigger>>>() {

			@Override
			public void handle(Message<LocalJsonObject<PushTrigger>> event) {
				LocalJsonObject<PushTrigger> msg = event.body();
				PushTrigger pt = msg.getValue();
				logger.debug("Received push trigger for {}", pt.collectionId);

				int count = 0;

				if (!pt.folderSyncRequired) {
					Collection<PushRegKey> matchingKeys = idxByCollectionId.get(pt.collectionId);
					Iterator<PushRegKey> it = matchingKeys.iterator();
					while (it.hasNext()) {
						PushRegKey pushKey = it.next();
						PushRegistrationRequest value = keyToReplyAddress.get(pushKey);
						if (value != null) {
							logger.info("Wake up request for collection {}, partnershipId {}", pushKey.collectionId,
									pushKey.partnershipId);
							eb.send(value.replyAddress, msg);
							vertx.cancelTimer(value.expirationTimerId);
							keyToReplyAddress.remove(pushKey);
							count++;
						}
						idxByPartnershipId.remove(pushKey.partnershipId, pushKey);
						idxByCollectionId.remove(pushKey.collectionId, pushKey);
					}
				} else {
					Collection<PushRegKey> matchingKeys = idxByPartnershipId.get(pt.userUid);
					Iterator<PushRegKey> it = matchingKeys.iterator();
					while (it.hasNext()) {
						PushRegKey pushKey = it.next();
						PushRegistrationRequest value = keyToReplyAddress.get(pushKey);
						if (value != null) {
							logger.info("FolderSyncRequired Wake up request for collection {}, partnershipId {}",
									pushKey.collectionId, pushKey.partnershipId);
							eb.send(value.replyAddress, msg);

							vertx.cancelTimer(value.expirationTimerId);
							keyToReplyAddress.remove(pushKey);
							count++;
						}
						idxByPartnershipId.remove(pushKey.partnershipId, pushKey);
						idxByCollectionId.remove(pushKey.collectionId, pushKey);
					}
				}

				if (logger.isDebugEnabled()) {
					logger.debug("Woke up {} queues for event on {}", count, pt.collectionId);
				}
			}

		};
		eb.registerHandler(EasBusEndpoints.PUSH_TRIGGER, triggerHandler);

		eb.registerHandler(EasBusEndpoints.PUSH_KILLER, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				Map<String, Object> m = event.body().toMap();
				Set<String> replyAddr = new HashSet<String>();
				for (String k : m.keySet()) {
					String partnershipId = (String) m.get(k);
					int collectionId = Integer.parseInt(k);
					Iterator<Entry<PushRegKey, PushRegistrationRequest>> it = keyToReplyAddress.entrySet().iterator();
					while (it.hasNext()) {
						Entry<PushRegKey, PushRegistrationRequest> entry = it.next();
						PushRegKey prk = entry.getKey();
						if (prk.collectionId == collectionId && prk.partnershipId.equals(partnershipId)) {
							PushRegistrationRequest value = entry.getValue();
							logger.debug("Unregister partnershipId {}, collectionId {}, reply-address {}",
									partnershipId, collectionId, value.replyAddress);
							replyAddr.add(value.replyAddress);
							vertx.cancelTimer(value.expirationTimerId);
							it.remove();
						}
					}
				}

				for (String addr : replyAddr) {
					eb.send(addr, new LocalJsonObject<>(PushTrigger.noChanges()));
				}

				event.reply();
			}

		});

		eb.registerHandler(EasBusEndpoints.PUSH_UNREGISTRATION, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				Iterator<Entry<PushRegKey, PushRegistrationRequest>> it = keyToReplyAddress.entrySet().iterator();
				while (it.hasNext()) {
					Entry<PushRegKey, PushRegistrationRequest> entry = it.next();
					vertx.cancelTimer(entry.getValue().expirationTimerId);
					it.remove();

					eb.send(entry.getValue().replyAddress, new LocalJsonObject<>(PushTrigger.noChanges()));
				}

			}

		});

	}

}
