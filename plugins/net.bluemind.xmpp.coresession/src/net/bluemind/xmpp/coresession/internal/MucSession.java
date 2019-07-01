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

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.muc.DefaultParticipantStatusListener;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.Occupant;
import org.jivesoftware.smackx.muc.ParticipantStatusListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

public class MucSession {

	private final Logger logger = LoggerFactory.getLogger(MucSession.class);
	private EventBus eventBus;
	private MultiUserChat muc;
	private String mucAddr;
	private Handler<Void> closeHandler;
	private XMPPConnection xmppConn;
	private PacketListener packedListener = new PacketListener() {

		@Override
		public void processPacket(Packet packet) throws NotConnectedException {
			if (packet instanceof org.jivesoftware.smack.packet.Message) {
				org.jivesoftware.smack.packet.Message msg = (org.jivesoftware.smack.packet.Message) packet;
				messageReceived(msg);
			} else {
				logger.warn("receive a packet not instanceof message");
			}
		}
	};

	public MucSession(EventBus eventBus, XMPPConnection xmppConn, String sessionId, MultiUserChat muc,
			Handler<Void> closeHandler) {
		this.eventBus = eventBus;
		this.muc = muc;
		mucAddr = "xmpp/muc/" + sessionId;
		this.closeHandler = closeHandler;
		this.xmppConn = xmppConn;
	}

	private void messageReceived(org.jivesoftware.smack.packet.Message msg) {
		if (!muc.getRoom().equals(msg.getFrom())) {
			logger.debug("[{}] message received for room {}, from {}, message '{}'", xmppConn.getUser(), muc.getRoom(),
					msg.getFrom(), msg.getBody());
			String from = msg.getFrom();
			String body = msg.getBody();
			eventBus.publish(mucAddr, MucSessionMessage.message(from, body));
		}
	}

	public void start() {

		muc.addMessageListener(packedListener);

		muc.addParticipantStatusListener(participantStatusListener);

		eventBus.registerHandler(mucAddr + "/" + muc.getRoom() + ":invite", inviteHandler);
		eventBus.registerHandler(mucAddr + "/" + muc.getRoom() + ":leave", leaveHandler);
		eventBus.registerHandler(mucAddr + "/" + muc.getRoom() + ":participants", participantsHandler);
		eventBus.registerHandler(mucAddr + "/" + muc.getRoom() + ":message", messageHandler);
	}

	public void stop() {
		muc.removeMessageListener(packedListener);
		muc.removeParticipantStatusListener(participantStatusListener);

		if (xmppConn.isConnected()) {
			try {
				muc.leave();
			} catch (NotConnectedException e) {
				logger.error(e.getMessage(), e);
			}
		}

		eventBus.unregisterHandler(mucAddr + "/" + muc.getRoom() + ":invite", inviteHandler);
		eventBus.unregisterHandler(mucAddr + "/" + muc.getRoom() + ":leave", leaveHandler);
		eventBus.unregisterHandler(mucAddr + "/" + muc.getRoom() + ":participants", participantsHandler);
		eventBus.unregisterHandler(mucAddr + "/" + muc.getRoom() + ":message", messageHandler);
	}

	private Handler<Message<JsonObject>> inviteHandler = new Handler<Message<JsonObject>>() {

		@Override
		public void handle(Message<JsonObject> msg) {
			JsonObject body = msg.body();

			logger.debug("[{}] sends invite to {}", xmppConn.getUser(), body.getString("latd"));
			String user = body.getString("latd");
			String reason = body.getString("reason");
			try {
				muc.invite(user, reason);
			} catch (NotConnectedException e) {
				logger.error("error during invite ", e);
			}
		}
	};

	private Handler<Message<JsonObject>> participantsHandler = new Handler<Message<JsonObject>>() {

		@Override
		public void handle(Message<JsonObject> msg) {
			try {
				List<Occupant> occs = new ArrayList<>();
				occs.addAll(muc.getParticipants());
				occs.addAll(muc.getModerators());
				msg.reply(MucSessionMessage.participants(occs));
			} catch (Exception e) {
				msg.reply(MucSessionMessage.error(e.getMessage()));
			}
		}

	};

	private Handler<Message<JsonObject>> leaveHandler = new Handler<Message<JsonObject>>() {

		@Override
		public void handle(Message<JsonObject> msg) {
			logger.debug("receive leave!");
			try {
				muc.leave();
				closeHandler.handle(null);
				msg.reply(MucSessionMessage.ok());
			} catch (NotConnectedException e) {
				msg.reply(MucSessionMessage.error(e.getMessage()));
			}
		}

	};
	private ParticipantStatusListener participantStatusListener = new DefaultParticipantStatusListener() {

		@Override
		public void nicknameChanged(String participant, String newNickname) {
			logger.debug("[{}] Room '{}'. {} has changed nickname to {}", xmppConn.getUser(), muc.getRoom(),
					participant, newNickname);

			notifyParticipantsChanged();
		}

		@Override
		public void left(String participant) {
			logger.debug("[{}] {} has left {}", xmppConn.getUser(), participant, muc.getRoom());
			eventBus.publish(mucAddr, MucSessionMessage.leave(muc.getRoom(), participant));

		}

		@Override
		public void kicked(String participant, String actor, String reason) {
			logger.debug("[{}] {} has been kicked from {}. Reason '{}'", xmppConn.getUser(), participant, muc.getRoom(),
					reason);
			notifyParticipantsChanged();

		}

		@Override
		public void joined(String participant) {
			logger.debug("[{}] {} has joined {}", xmppConn.getUser(), participant, muc.getRoom());
			eventBus.publish(mucAddr, MucSessionMessage.join(muc.getRoom(), participant));
		}

	};

	protected void notifyParticipantsChanged() {
		eventBus.publish(mucAddr, MucSessionMessage.participantsChanged(muc.getRoom()));
	}

	private Handler<Message<JsonObject>> messageHandler = new Handler<Message<JsonObject>>() {

		@Override
		public void handle(Message<JsonObject> msg) {
			try {
				logger.debug("[{}] sends message to {}: '{}'", xmppConn.getUser(), muc.getRoom(),
						msg.body().getString("message"));
				muc.sendMessage(msg.body().getString("message"));
			} catch (NotConnectedException | XMPPException e) {
				msg.reply(MucSessionMessage.error(e.getMessage()));
			}
		}
	};
}
