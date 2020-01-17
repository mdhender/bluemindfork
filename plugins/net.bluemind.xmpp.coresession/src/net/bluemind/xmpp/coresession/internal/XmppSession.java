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

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jivesoftware.smack.AbstractConnectionListener;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.muc.HostedRoom;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;

public class XmppSession {

	private static final Logger logger = LoggerFactory.getLogger(XmppSession.class);

	private static final int PRIORITY = 1;

	private XMPPConnection xmppConn;
	private EventBus eventBus;
	private String busAddr;
	private String sessionId;
	private ChatManager chatManager;
	private Presence presence;
	private MucSessions mucManager;
	private List<String> chatThreadList;
	private RosterManager roster;

	private Set<org.jivesoftware.smack.packet.Message> unread;
	private Set<String> pendingSubscription;
	private List<MessageConsumer<?>> consumers;

	public XmppSession(XMPPConnection connection, String sessionId, Vertx vertx) {

		this.sessionId = sessionId;
		eventBus = vertx.eventBus();
		busAddr = "xmpp/session/" + sessionId;

		xmppConn = connection;
		xmppConn.addConnectionListener(connectionListener());

		mucManager = new MucSessions(eventBus, sessionId, xmppConn);

		roster = new RosterManager(vertx, sessionId, connection);
		chatManager = ChatManager.getInstanceFor(xmppConn);

		chatThreadList = new ArrayList<>();

		unread = new LinkedHashSet<org.jivesoftware.smack.packet.Message>();
		pendingSubscription = new HashSet<String>();
		this.consumers = new LinkedList<>();
		registerHandlers();

	}

	private PacketListener errorMessageListener = new PacketListener() {

		@Override
		public void processPacket(Packet packet) throws NotConnectedException {

			org.jivesoftware.smack.packet.Message message = (org.jivesoftware.smack.packet.Message) packet;
			if (message.getError() != null) {
				logger.error("[{}] receives error message '{}'", xmppConn.getUser(), packet.getError().getMessage());
				eventBus.publish(busAddr + "/error",
						XmppSessionMessage.errorMessage(message.getThread(), message.getError().getMessage()));
			}

		}
	};

	private PacketListener presenceSubscribeListener = new PacketListener() {
		@Override
		public void processPacket(Packet pack) {
			Presence pres = (Presence) pack;

			if (pres.getType() != null) {

				if (pres.getType().equals(Presence.Type.subscribe)) {

					logger.debug("[{}] {} asks for prescence subscription", xmppConn.getUser(), pres.getFrom());

					eventBus.publish(busAddr, message("presence", "subscribe").put("body", new JsonObject()//
							.put("from", pres.getFrom())));

					// send notification
					String from = pres.getFrom();
					String pic = "";
					RosterItem item = RosterItemCache.getInstance().get(XmppSessionMessage.parseJabberId(from));
					if (item != null) {
						from = item.user.value.contactInfos.identification.formatedName.value;
						pic = item.photo;
					}

					pendingSubscription.add(pres.getFrom());

					eventBus.publish(busAddr + "/notification", message("presence", "subscribe").put("body",
							new JsonObject().put("from", from).put("pic", pic)));

				} else if (pres.getType().equals(Presence.Type.available)) {
					if (xmppConn.getUser().equals(pres.getFrom())) {
						if (pres.getMode() == null) {
							pres.setMode(Mode.available);
						}
						presence = pres;

						logger.debug("[{}] Presence change to {} > {}", xmppConn.getUser(), presence.getMode(),
								presence.getStatus());

						pushState();
					}
				}

			} else if (pres.getType() != null && pres.getType().equals(Presence.Type.unsubscribe)) {
				Presence presence = new Presence(Presence.Type.unsubscribe);
				presence.setTo(pres.getFrom());
				try {
					xmppConn.sendPacket(presence);
				} catch (NotConnectedException e) {
					logger.error("error during unsubscription", e);
				}

			}
		}
	};

	private void registerHandlers() {
		chatManager.addChatListener(chatListener);

		xmppConn.addPacketListener(errorMessageListener,
				new PacketTypeFilter(org.jivesoftware.smack.packet.Message.class));

		xmppConn.addPacketListener(presenceSubscribeListener, new PacketTypeFilter(Presence.class));

		consumers.add(eventBus.consumer(busAddr + ":ownPresence", ownPresenceHandler));
		consumers.add(eventBus.consumer(busAddr + ":presence", presenceHandler));
		consumers.add(eventBus.consumer(busAddr + ":close", closeHandler));
		consumers.add(eventBus.consumer(busAddr + ":chat", chatHandler));
		consumers.add(eventBus.consumer(busAddr + ":accept-subscribe", acceptSubscribeHandler));

		consumers.add(eventBus.consumer(busAddr + ":discard-subscribe", discardSubscribeHandler));
		consumers.add(eventBus.consumer(busAddr + ":ask-subscribe", askSubscribeHandler));

		consumers.add(eventBus.consumer(busAddr + ":unread", unreadHandler));
		consumers.add(eventBus.consumer(busAddr + ":mark-all-as-read", markAllAsReadHandler));

		roster.start();
		mucManager.start();
	}

	private void unregisterHandlers() {
		chatManager.removeChatListener(chatListener);

		xmppConn.removePacketListener(errorMessageListener);

		xmppConn.removePacketListener(presenceSubscribeListener);

		consumers.forEach(MessageConsumer::unregister);

		for (String thread : chatThreadList) {
			// eventBus.unregisterHandler(busAddr + "/chat/" + thread + ":message",
			// messageHandler);

			Chat chat = chatManager.getThreadChat(thread);
			if (chat != null) {
				chat.removeMessageListener(messageListener);
			}
		}

		mucManager.stop();
		roster.stop();
	}

	private Handler<Message<JsonObject>> unreadHandler = new Handler<Message<JsonObject>>() {

		@Override
		public void handle(Message<JsonObject> event) {

			logger.debug("[{}] Presence is {}", xmppConn.getUser(), XmppSession.this.presence);

			JsonObject body = event.body();
			String origin = body.getString("origin");

			if ("im".equals(origin)) {
				if (unread.size() > 0) {
					logger.debug("[{}] load {} unread chat.", xmppConn.getUser(), unread.size());

					for (org.jivesoftware.smack.packet.Message m : unread) {
						logger.debug("[{}] send unread message from {}: '{}'", xmppConn.getUser(), m.getFrom(),
								m.getBody());
						eventBus.publish(busAddr + "/unread",
								XmppSessionMessage.message(m.getThread(), m.getFrom(), m.getBody()));
					}
					unread.clear();
				}

				if (pendingSubscription.size() > 0) {
					logger.debug("[{}] load {} pending subscription request", xmppConn.getUser(),
							pendingSubscription.size());

					for (String from : pendingSubscription) {
						eventBus.publish(busAddr,
								message("presence", "subscribe").put("body", new JsonObject().put("from", from)));

					}

					pendingSubscription.clear();
				}
			} else if ("push".equals(origin)) {
				if (unread.size() > 0 || pendingSubscription.size() > 0) {
					logger.debug("[{}] send unread to bm-push", xmppConn.getUser());
					eventBus.publish(busAddr + "/notification", XmppSessionMessage.blinkNotification());
				}
			}
		}

	};

	private Handler<Message<JsonObject>> markAllAsReadHandler = new Handler<Message<JsonObject>>() {

		@Override
		public void handle(Message<JsonObject> event) {
			logger.debug("[{}] mark all as read.", xmppConn.getUser());
			unread.clear();
			eventBus.publish(busAddr + "/notification", XmppSessionMessage.markAllAsRead());
		}

	};

	private Handler<Message<JsonObject>> closeHandler = new Handler<Message<JsonObject>>() {

		@Override
		public void handle(Message<JsonObject> event) {
			close();
		}
	};

	public void close() {
		try {
			logger.debug("[{}] disconnect xmpp session {}", xmppConn.getUser(), sessionId);
			xmppConn.disconnect();
			unregisterHandlers();
			xmppConn = null;

			eventBus.send("xmpp/sessions-manager:internal-close", new JsonObject().put("sessionId", sessionId));

			presence = new Presence(Presence.Type.unavailable);
			pushState();
		} catch (Exception e) {
			logger.error("error on closing connection ", e);
		}
	}

	/**
	 * call :ownPresenceHandler to fetch current connection presence
	 */
	private Handler<Message<JsonObject>> ownPresenceHandler = new Handler<Message<JsonObject>>() {

		@Override
		public void handle(Message<JsonObject> event) {
			Presence presence = XmppSession.this.presence;
			if (presence != null) {
				pushState();
			}
		}

	};

	/**
	 * call :presence to change current connection presence
	 */
	private Handler<Message<JsonObject>> presenceHandler = new Handler<Message<JsonObject>>() {

		@Override
		public void handle(Message<JsonObject> event) {

			JsonObject body = event.body();

			Mode mode = null;
			try {
				mode = Mode.valueOf(body.getString("mode"));
			} catch (Exception e) {
				logger.error("invalid mode");
				return;
			}
			Presence presence = new Presence(Presence.Type.available, body.getString("status"), PRIORITY, mode);

			// ask server to change presence
			try {
				xmppConn.sendPacket(presence);
			} catch (NotConnectedException e) {
				logger.error("error during status change");
			}
		}
	};

	/**
	 * call :chat to create new chat between two users
	 */
	private Handler<Message<JsonObject>> chatHandler = new Handler<Message<JsonObject>>() {

		@Override
		public void handle(Message<JsonObject> event) {
			JsonObject body = event.body();

			String userJID = body.getString("userJID");

			if (userJID == null || userJID.isEmpty()) {
				logger.error("[{}] Fail to create chat. userJID cannot be null.", xmppConn.getUser());

				eventBus.publish(busAddr, XmppSessionMessage.chatCreationFailed());

				return;
			}

			String jabberId = XmppSessionMessage.parseJabberId(xmppConn.getUser());

			String threadId = jabberId + "#" + userJID;
			if (jabberId.compareTo(userJID) > 0) {
				threadId = userJID + "#" + jabberId;
			}

			Chat chat = chatManager.getThreadChat(threadId);

			if (chat == null) {
				logger.debug("[{}] create chat with threadID {}", xmppConn.getUser(), threadId);

				MessageListener tempListener = new MessageListener() {

					@Override
					public void processMessage(Chat chat, org.jivesoftware.smack.packet.Message message) {

					}
				};
				chat = chatManager.createChat(userJID, threadId, tempListener);

				chat.removeMessageListener(tempListener);
			} else {
				eventBus.publish(busAddr, XmppSessionMessage.chatCreationOk(chat));
			}

			String message = body.getString("message");
			if (message != null) {
				sendMessage(chat, message);
			}

		}
	};

	/**
	 * call /chat/threadID:message to send a message
	 */
	private Handler<Message<JsonObject>> messageHandler = new Handler<Message<JsonObject>>() {

		@Override
		public void handle(Message<JsonObject> event) {
			JsonObject body = event.body();
			String message = body.getString("message");

			// guess thread id from address
			// xmpp/session/SESSION_ID/chat/THREAD_ID:message

			String address[] = event.address().split("/");
			String threadID = address[address.length - 1].split(":")[0];
			Chat chat = chatManager.getThreadChat(threadID);
			sendMessage(chat, message);
		}

	};

	private void sendMessage(Chat chat, String message) {
		try {
			chat.sendMessage(message);

			eventBus.publish(busAddr, XmppSessionMessage.message(chat.getThreadID(), xmppConn.getUser(), message));

			logger.debug("[{}] Send message to: {}, threadID: {}, msg: '{}'", xmppConn.getUser(), chat.getParticipant(),
					chat.getThreadID(), message);
		} catch (NotConnectedException | XMPPException e) {
			logger.error("[{}] Fail to send message to {}", xmppConn.getUser(), chat.getParticipant(), e);
		}
	}

	private Handler<Message<JsonObject>> acceptSubscribeHandler = new Handler<Message<JsonObject>>() {

		@Override
		public void handle(Message<JsonObject> request) {
			JsonObject msg = request.body();
			logger.debug("[{}] request accept subscribe {}", xmppConn.getUser(), msg.getString("to"));

			Presence presence = new Presence(Presence.Type.subscribed);
			presence.setTo(msg.getString("to"));
			try {
				xmppConn.sendPacket(presence);
				request.reply(XmppSessionMessage.ok());
			} catch (NotConnectedException e) {
				logger.error("error during presence subscribe acceptation ", e);
				request.reply(XmppSessionMessage.error(e.getMessage()));
			}
		}

	};

	private Handler<Message<JsonObject>> discardSubscribeHandler = new Handler<Message<JsonObject>>() {

		@Override
		public void handle(Message<JsonObject> request) {
			JsonObject msg = request.body();
			logger.debug("request discard subscribe {}", msg.getString("to"));

			Presence presence = new Presence(Presence.Type.unsubscribed);
			presence.setTo(msg.getString("to"));
			try {
				xmppConn.sendPacket(presence);
				request.reply(XmppSessionMessage.ok());
			} catch (NotConnectedException e) {
				logger.error("error during presence subscribe acceptation ", e);
				request.reply(XmppSessionMessage.error(e.getMessage()));
			}
		}

	};

	private Handler<Message<JsonObject>> askSubscribeHandler = new Handler<Message<JsonObject>>() {

		@Override
		public void handle(Message<JsonObject> request) {
			JsonObject msg = request.body();
			logger.debug("[{}] request ask subscribe {}", xmppConn.getUser(), msg.getString("to"));

			Presence presence = new Presence(Presence.Type.subscribe);
			presence.setFrom(xmppConn.getUser());
			presence.setTo(msg.getString("to"));
			try {
				xmppConn.sendPacket(presence);
				request.reply(XmppSessionMessage.ok());
			} catch (NotConnectedException e) {
				logger.error("error during presence subscribe acceptation ", e);
				request.reply(XmppSessionMessage.error(e.getMessage()));
			}
		}

	};

	private ChatManagerListener chatListener = new ChatManagerListener() {

		@Override
		public void chatCreated(Chat chat, boolean createdLocally) {
			String threadID = chat.getThreadID();
			String participant = chat.getParticipant();

			// FIXME
			try {
				Collection<HostedRoom> rooms = MultiUserChat.getHostedRooms(xmppConn,
						"muc." + xmppConn.getServiceName());
				for (HostedRoom room : rooms) {
					if (room.getJid().equals(participant)) {
						logger.debug("Bloody hell");
						return;
					}
				}
			} catch (NoResponseException | XMPPErrorException | NotConnectedException e) {
				logger.error(e.getMessage(), e);
			}

			logger.debug("[{}] New chat with {}. ThreadID: {}", xmppConn.getUser(), participant, threadID);

			chat.addMessageListener(messageListener);

			eventBus.consumer(busAddr + "/chat/" + threadID + ":message", messageHandler);

			chatThreadList.add(threadID);

			eventBus.publish(busAddr, XmppSessionMessage.chatCreationOk(chat));
		}

	};

	private MessageListener messageListener = new MessageListener() {

		@Override
		public void processMessage(Chat chat, org.jivesoftware.smack.packet.Message message) {

			if (message.getBody() != null) {
				logger.debug("[{}] New message from {}, thread {}: '{}'", xmppConn.getUser(), chat.getParticipant(),
						chat.getThreadID(), message.getBody());

				unread.add(message);

				eventBus.publish(busAddr,
						XmppSessionMessage.message(chat.getThreadID(), chat.getParticipant(), message.getBody()));

				// send notification
				String from = chat.getParticipant();
				String pic = "";
				String jabberId = XmppSessionMessage.parseJabberId(from);

				RosterItem item = RosterItemCache.getInstance().get(jabberId);
				if (item != null) {
					from = item.user.value.contactInfos.identification.formatedName.value;
					pic = item.photo;
				}
				eventBus.publish(busAddr + "/notification",
						XmppSessionMessage.messageNotification(chat.getThreadID(), from, pic, message.getBody()));

			}
		}
	};

	private ConnectionListener connectionListener() {
		return new AbstractConnectionListener() {
			@Override
			public void authenticated(XMPPConnection connection) {
				eventBus.publish(busAddr, message("connection", "authenticated"));
			}
		};
	}

	public void authenticate(String login, String password) throws XMPPException, IOException, SmackException {
		logger.debug("Auth: login '{}' password '{}'", login, password);
		xmppConn.login(login, password, "BlueMind_" + System.currentTimeMillis());
	}

	private JsonObject message(String category, String action) {
		JsonObject message = new JsonObject();
		message.put("category", category);
		message.put("action", action);
		return message;
	}

	public static XmppSession create(String host, int port, String serviceName, String sessionId, Vertx vertx)
			throws Exception {
		ConnectionConfiguration config = new ConnectionConfiguration(host, port, serviceName);

		// trust all
		SSLContext sc = SSLContext.getInstance("TLS");
		HostnameVerifier ver = new HostnameVerifier() {

			public boolean verify(String hostname, SSLSession session) {
				return true;
			}

		};
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			}
		} };

		config.setHostnameVerifier(ver);
		sc.init(null, trustAllCerts, new SecureRandom());

		config.setCustomSSLContext(sc);
		config.setSecurityMode(SecurityMode.disabled);
		// config.setDebuggerEnabled(true);// crashes on Mac
		XMPPTCPConnection xmppConn = new XMPPTCPConnection(config);
		xmppConn.getRoster();
		xmppConn.connect();
		return new XmppSession(xmppConn, sessionId, vertx);
	}

	private void pushState() {
		eventBus.publish(busAddr, new JsonObject().put("category", "ownPresence").put("presence",
				XmppSessionMessage.presence(presence.getType(), presence.getStatus(), presence.getMode())));

	}
}
