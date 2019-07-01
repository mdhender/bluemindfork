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
package net.bluemind.xmpp.coresession.internal;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.Roster.SubscriptionMode;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class RosterManager {

	private static final Logger logger = LoggerFactory.getLogger(RosterManager.class);

	private EventBus eventBus;
	private String busAddr;

	private Roster roster;

	private XMPPConnection xmppConn;

	protected long cleanupTimer = -1;
	private Vertx vertx;

	public RosterManager(Vertx vertx, String sessionId, XMPPConnection xmppConn) {
		this.vertx = vertx;
		this.eventBus = vertx.eventBus();
		busAddr = "xmpp/session/" + sessionId + "/roster";
		this.xmppConn = xmppConn;
		this.roster = xmppConn.getRoster();
		// user have to accept subscription
		roster.setSubscriptionMode(SubscriptionMode.manual);

		uglyCleanup();
	}

	private void uglyCleanup() {

		try {
			Field field = roster.getClass().getDeclaredField("presenceMap");
			field.setAccessible(true);
			@SuppressWarnings("unchecked")
			Map<String, ConcurrentHashMap<String, Presence>> value = (Map<String, ConcurrentHashMap<String, Presence>>) field
					.get(roster);
			// every five minutes
			// cleanup unavailable entries
			cleanupTimer = vertx.setPeriodic(1000 * 60 * 5, new Handler<Long>() {

				@Override
				public void handle(Long event) {
					logger.debug("cleanup unavailable entries {}", value);
					RosterManager.this.cleanupTimer = event;
					try {
						value.entrySet().stream().forEach(u -> {

							u.getValue().entrySet().removeIf(p -> {
								return p.getValue().getType() == Type.unavailable;
							});
						});

						value.entrySet().removeIf(v -> v.getValue().isEmpty());
					} catch (Exception e) {
						logger.error("error during cleanup (concurrent modification ?", e);
					}
				}
			});
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			logger.error("error ", e);
		}
	}

	public void start() {
		roster.addRosterListener(rosterListener);

		eventBus.registerHandler(busAddr + ":entries", entriesHandler);
		eventBus.registerHandler(busAddr + ":add-buddy", addBuddyHandler);
		eventBus.registerHandler(busAddr + ":remove-buddy", removeBuddyHandler);
	}

	public void stop() {
		if (cleanupTimer != -1) {
			vertx.cancelTimer(cleanupTimer);
		}
		roster.removeRosterListener(rosterListener);
		eventBus.unregisterHandler(busAddr + ":entries", entriesHandler);
		eventBus.unregisterHandler(busAddr + ":add-buddy", addBuddyHandler);
		eventBus.unregisterHandler(busAddr + ":remove-buddy", removeBuddyHandler);

	}

	private Handler<Message<JsonObject>> addBuddyHandler = new Handler<Message<JsonObject>>() {

		@Override
		public void handle(Message<JsonObject> request) {
			logger.debug("[{}] add buddy called", xmppConn.getUser());
			String user = request.body().getString("user");
			try {
				roster.createEntry(user, user, new String[] {});
				logger.debug("[{}] {} added", xmppConn.getUser(), user);
				request.reply(RosterMessage.ok());

			} catch (Exception e) {
				logger.error("[{}] error adding buddy {}", xmppConn.getUser(), user, e);
				request.reply(RosterMessage.error(e.getMessage()));

			}

		}
	};

	private Handler<Message<JsonObject>> removeBuddyHandler = new Handler<Message<JsonObject>>() {

		@Override
		public void handle(Message<JsonObject> request) {
			logger.debug("[{}] remove buddy called", xmppConn.getUser());
			String user = request.body().getString("user");
			try {
				RosterEntry entry = roster.getEntry(user);
				if (entry == null) {
					request.reply(RosterMessage.error("buddy " + user + " not a buddy"));
				} else {

					Presence presence = new Presence(Presence.Type.unsubscribed);
					presence.setTo(user);
					xmppConn.sendPacket(presence);
					request.reply(RosterMessage.ok());

					roster.removeEntry(entry);
					logger.debug("[{}] {} removed", xmppConn.getUser(), user);

				}

			} catch (Exception e) {
				logger.error("error adding buddy ", e);
				request.reply(RosterMessage.error(e.getMessage()));

			}

		}
	};

	private Handler<Message<JsonObject>> entriesHandler = new Handler<Message<JsonObject>>() {

		@Override
		public void handle(Message<JsonObject> msg) {

			logger.debug("[{}] roster:entries called {}", xmppConn.getUser(), msg.body());

			JsonArray entries = msg.body().getArray("entries");
			JsonArray array = new JsonArray();

			if (entries != null) {

				for (Object entryO : entries) {
					JsonObject re = rosterEntry(roster.getEntry(((String) entryO)));
					if (re != null) {
						array.add(re);
					}
				}
			} else {

				for (RosterEntry entry : roster.getEntries()) {
					JsonObject re = rosterEntry(entry);
					if (re != null) {
						array.add(re);
					}
				}
			}

			JsonObject replyMsg = new JsonObject().putArray("entries", array);
			msg.reply(replyMsg);
		}

		private JsonObject rosterEntry(RosterEntry entry) {
			RosterItem item = RosterItemCache.getInstance().get(entry.getUser());

			if (item != null) {
				JsonObject ret = new JsonObject();
				ret.putString("user", entry.getUser());
				ret.putString("userUid", item.user.uid);
				ret.putString("name", item.user.displayName);
				ret.putString("latd", item.user.value.defaultEmail().address);
				if (item.photo != null) {
					ret.putString("photo", item.photo);
				}

				Presence userPresence = roster.getPresence(entry.getUser());
				JsonObject presence = new JsonObject();
				presence.putString("type", userPresence.getType().name());
				if (userPresence.getMode() != null) {
					presence.putString("mode", userPresence.getMode().name());
				}
				if (userPresence.getStatus() != null) {
					presence.putString("status", userPresence.getStatus());
				}
				ret.putObject("presence", presence);

				if (entry.getType() != null) {
					ret.putString("subs", entry.getType().name());
				}

				logger.debug("[{}] roster entry: {} / {}, presence {}, subs: {}", xmppConn.getUser(),
						ret.getString("user"), ret.getString("name"), ret.getObject("presence"), ret.getString("subs"));

				return ret;
			}

			logger.debug("[{}] roster entry {} does not exist", xmppConn.getUser(), entry.getUser());
			return null;

		}
	};
	private RosterListener rosterListener = new RosterListener() {

		@Override
		public void presenceChanged(Presence presence) {
			String jabberId = XmppSessionMessage.parseJabberId(presence.getFrom());
			Presence bestPresence = roster.getPresence(jabberId);

			logger.debug("[{}] {} has changed: presence : {}. Best presence for {}: {}", xmppConn.getUser(),
					presence.getFrom(), presence, jabberId, bestPresence);

			eventBus.send(busAddr, RosterMessage.presenceChanged(bestPresence));

		}

		@Override
		public void entriesUpdated(Collection<String> addresses) {
			logger.debug("roster of {} has changed: entries updated : {} ", xmppConn.getUser(), addresses);
			eventBus.send(busAddr, RosterMessage.entriesUpdated(addresses));
		}

		@Override
		public void entriesDeleted(Collection<String> addresses) {
			logger.debug("roster of {} has changed: entries deleted : {} ", xmppConn.getUser(), addresses);
			eventBus.send(busAddr, RosterMessage.entriesDeleted(addresses));

		}

		@Override
		public void entriesAdded(Collection<String> addresses) {
			logger.debug("roster of {} has changed: entries added : {} ", xmppConn.getUser(), addresses);
			eventBus.send(busAddr, RosterMessage.entriesAdded(addresses));
		}
	};

}
