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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smackx.muc.InvitationListener;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

public class MucSessions {

	private static final Logger logger = LoggerFactory.getLogger(MucSessions.class);

	private XMPPConnection xmppConn;
	private String sessionId;
	private EventBus eventBus;
	private String busAddr;
	private ConcurrentHashMap<String, MucSession> mucs = new ConcurrentHashMap<>();
	private Map<String, PendingMucInvite> pending;

	public MucSessions(EventBus eventBus, String sessionId, XMPPConnection xmppConn) {
		this.eventBus = eventBus;
		this.sessionId = sessionId;
		this.xmppConn = xmppConn;

		busAddr = "xmpp/muc/" + sessionId;

		pending = new HashMap<String, PendingMucInvite>();

	}

	public void start() {
		MultiUserChat.addInvitationListener(xmppConn, invitationListener);
		eventBus.registerHandler(busAddr + ":pending", pendingHandler);
		eventBus.registerHandler(busAddr + ":create", createHandler);
		eventBus.registerHandler(busAddr + ":join", joinHandler);
		eventBus.registerHandler(busAddr + ":close", closeHandler);
	}

	public void stop() {
		MultiUserChat.removeInvitationListener(xmppConn, invitationListener);
		eventBus.unregisterHandler(busAddr + ":pending", pendingHandler);
		eventBus.unregisterHandler(busAddr + ":create", createHandler);
		eventBus.unregisterHandler(busAddr + ":join", joinHandler);
		eventBus.unregisterHandler(busAddr + ":close", closeHandler);

		for (MucSession muc : mucs.values()) {
			muc.stop();
		}
	}

	private Handler<Message<JsonObject>> closeHandler = new Handler<Message<JsonObject>>() {

		@Override
		public void handle(Message<JsonObject> event) {
			// Leave all MUC
			for (MucSession muc : mucs.values()) {
				logger.debug("[{}] leave muc {}", xmppConn.getUser(), muc);
				muc.stop();
			}
			mucs.clear();
		}
	};

	private Handler<Message<JsonObject>> pendingHandler = new Handler<Message<JsonObject>>() {

		@Override
		public void handle(Message<JsonObject> event) {
			JsonObject body = event.body();
			String origin = body.getString("origin");

			if ("im".equals(origin)) {
				logger.debug("[{}] load {} pending muc invite.", xmppConn.getUser(), pending.size());

				for (PendingMucInvite p : pending.values()) {
					logger.debug("[{}] muc invitation for room {}", xmppConn.getUser(), p.room);
					eventBus.publish(busAddr + "/pending",
							XmppSessionMessage.mucInvitation(p.room, p.inviter, p.reason));
				}
				pending.clear();

				logger.debug("[{}] load {} current muc", xmppConn.getUser(), mucs.size());
				String nickname = XmppSessionMessage.parseJabberId(xmppConn.getUser());
				for (String roomName : mucs.keySet()) {
					// stop current muc
					MucSession ms = mucs.get(roomName);
					ms.stop();

					// start new muc
					try {
						MultiUserChat muc = new MultiUserChat(xmppConn, roomName);
						mucSession(roomName, muc);
						logger.debug("[{}] {} joins room {}", xmppConn.getUser(), nickname, roomName);
						muc.join(nickname);
					} catch (NoResponseException | XMPPErrorException | NotConnectedException e) {
						logger.error(e.getMessage(), e);
					}
				}
			} else if ("push".equals(origin)) {
				if (pending.size() > 0) {
					logger.debug("[{}] send pending muc to bm-push", xmppConn.getUser());
					eventBus.publish(busAddr + "/notification", XmppSessionMessage.blinkNotification());
				}
			}

		}
	};

	private InvitationListener invitationListener = new InvitationListener() {

		@Override
		public void invitationReceived(XMPPConnection conn, String room, String inviter, String reason, String password,
				org.jivesoftware.smack.packet.Message message) {

			logger.debug("[{}] receive invite {}: {}", xmppConn.getUser(), inviter, reason);

			PendingMucInvite p = new PendingMucInvite();
			p.room = room;
			p.inviter = inviter;
			p.reason = reason;

			logger.debug("[{}] add {} to pending muc", xmppConn.getUser(), room);

			pending.put(room, p);

			eventBus.publish(busAddr, XmppSessionMessage.mucInvitation(room, inviter, reason));

			// notification
			String from = inviter;
			String pic = "";
			String jabberId = XmppSessionMessage.parseJabberId(inviter);
			RosterItem item = RosterItemCache.getInstance().get(jabberId);
			if (item != null) {
				from = item.user.value.contactInfos.identification.formatedName.value;
				pic = item.photo;
			}

			eventBus.publish(busAddr + "/notification",
					XmppSessionMessage.mucInvitationNotification(room, from, pic, reason));

		}
	};

	private Handler<Message<JsonObject>> createHandler = new Handler<Message<JsonObject>>() {

		@Override
		public void handle(Message<JsonObject> msg) {

			logger.debug("[{}] create room {}", xmppConn.getUser(), msg.body());
			JsonObject body = msg.body();

			// FIXME maybe we should discovery muc service ?
			final String roomName = body.getString("name") + "@muc." + xmppConn.getServiceName();
			MultiUserChat muc = new MultiUserChat(xmppConn, roomName);
			try {
				logger.debug("[{}] create room {} by {}", xmppConn.getUser(), roomName, body.getString("nickname"));

				muc.createOrJoin(body.getString("nickname"));

				mucSession(roomName, muc);
				logger.debug("[{}] reply ok for room creation {}", xmppConn.getUser(), body.getString("name"));
				msg.reply(XmppSessionMessage.mucCreationOk(roomName));
			} catch (Exception e) {
				logger.error("error during room creation", e);
				msg.reply(XmppSessionMessage.mucCreationFailed(e.getMessage()));
			}
		}
	};

	private Handler<Message<JsonObject>> joinHandler = new Handler<Message<JsonObject>>() {

		@Override
		public void handle(Message<JsonObject> msg) {
			JsonObject body = msg.body();
			String room = body.getString("room");
			String nickname = body.getString("nickname");
			MultiUserChat muc = new MultiUserChat(xmppConn, room);
			try {
				mucSession(room, muc);
				logger.debug("[{}] {} joins room {}", xmppConn.getUser(), nickname, room);
				muc.join(nickname);
				msg.reply(XmppSessionMessage.mucJoinOk());
			} catch (NoResponseException | XMPPErrorException | NotConnectedException e) {
				logger.error("error during joining room", e);
				msg.reply(XmppSessionMessage.error(e.getMessage()));
			}
		}
	};

	private Handler<Void> closeHandler(final String roomName) {

		return new Handler<Void>() {

			@Override
			public void handle(Void event) {
				MucSession session = mucs.remove(roomName);
				if (session != null) {
					session.stop();
				}
			}

		};
	}

	private void mucSession(String roomName, MultiUserChat muc) {
		logger.debug("[{}] new muc session room {}", xmppConn.getUser(), roomName);
		MucSession mucSession = new MucSession(eventBus, xmppConn, sessionId, muc, closeHandler(roomName));
		mucSession.start();
		pending.remove(roomName);
		mucs.put(roomName, mucSession);
	}
}
