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
package net.bluemind.ui.im.client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.im.api.IInstantMessagingAsync;
import net.bluemind.im.api.IMMessage;
import net.bluemind.im.api.gwt.endpoint.InstantMessagingGwtEndpoint;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.im.client.chatroom.InviteToChatroom;
import net.bluemind.ui.im.client.chatroom.NewInvitation;
import net.bluemind.ui.im.client.chatroom.SendHistory;
import net.bluemind.ui.im.client.conversation.BMMessageEvent;
import net.bluemind.ui.im.client.conversation.Conversation;
import net.bluemind.ui.im.client.conversation.ConversationMessageHandler;
import net.bluemind.ui.im.client.conversation.ConversationMessageListener;
import net.bluemind.ui.im.client.conversation.Conversations;
import net.bluemind.ui.im.client.conversation.CreateConversation;
import net.bluemind.ui.im.client.conversation.MucConversation;
import net.bluemind.ui.im.client.leftpanel.LeftPanel;
import net.bluemind.ui.im.client.leftpanel.RosterItem;
import net.bluemind.ui.im.client.push.MessageHandler;
import net.bluemind.ui.im.client.push.Push;
import net.bluemind.ui.im.client.push.message.ChatMessage;
import net.bluemind.ui.im.client.push.message.ErrorMessage;
import net.bluemind.ui.im.client.push.message.MucMessage;
import net.bluemind.ui.im.client.push.message.PhoneMessage;
import net.bluemind.ui.im.client.push.message.PresenceMessage;
import net.bluemind.ui.im.client.push.message.RosterMessage;
import net.bluemind.ui.im.client.subscription.AddToFavorites;
import net.bluemind.ui.im.client.subscription.RemoveFromFavorites;
import net.bluemind.ui.im.client.subscription.SubscriptionRequest;

public class IMCtrl {
	private static final IMCtrl instance;
	private Map<String, IScreen> screens;
	private ConversationMessageListener cml;

	static {
		instance = new IMCtrl();
	}

	public static IMCtrl getInstance() {
		return instance;
	}

	private IMCtrl() {
		screens = new HashMap<String, IScreen>();
		cml = new ConversationMessageListener();
	}

	/**
	 * @param id
	 * @param c
	 */
	public void registerScreen(String id, IScreen c) {
		screens.put(id, c);
	}

	/**
	 * @param c
	 * @param focus
	 */
	public void registerConversation(Conversation c, boolean focus) {
		Conversations conversations = getConversationsScreen();
		conversations.register(c, focus);
	}

	/**
	 * @param c
	 */
	public void unregisterConversation(Conversation c) {

		if (c instanceof MucConversation) {
			leaveMuc(c.getJabberId());
		}

		Conversations conversations = getConversationsScreen();
		conversations.unregister(c);
	}

	public void sendMessage(String id, String msg) {
		Conversations conversations = getConversationsScreen();
		Conversation conv = conversations.getFromId(id);
		if (conv != null) {
			conv.sendMessage(msg);
		}
	}

	public void markAsRead(String id) {
		cml.markAsRead(id);
	}

	public void addMessageReceivedEventHandler(ConversationMessageHandler handler) {
		cml.addMessageReceivedEventHandler(handler);
	}

	public void addMarkAsReadEventHandler(ConversationMessageHandler handler) {
		cml.addMarkAsReadEventHandler(handler);
	}

	/**
	 * @param room
	 */
	public void showSendHistoryDialog(String roomName) {
		SendHistory ce = (SendHistory) screens.get(Screens.SEND_HISTORY);
		ce.setRoomName(roomName);
		ce.center();
		ce.show();
	}

	/**
	 * @param room
	 * @param recipients
	 */
	public void sendHistory(final String roomName, final String recipients) {
		Conversations conversations = getConversationsScreen();

		final Conversation conv = conversations.getFromId(roomName);
		IInstantMessagingAsync im = new InstantMessagingGwtEndpoint(Ajax.TOKEN.getSessionId());
		im.sendGroupChatHistory(Ajax.getDefaultEmail(), roomName, Arrays.asList(recipients), new AsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				conv.info(IMConstants.INST.groupChatHistoryWasSentTo(recipients));
			}

			@Override
			public void failure(Throwable e) {
			}
		});

	}

	public LeftPanel getLeftPanelScreen() {
		return (LeftPanel) screens.get(Screens.LEFT_PANEL);
	}

	public Conversations getConversationsScreen() {
		return (Conversations) screens.get(Screens.CONVERSATIONS);
	}

	public CreateConversation getCreateConversationsScreen() {
		return (CreateConversation) screens.get(Screens.CREATE_CONVERSATION);
	}

	public InviteToChatroom getInviteToChatroomScreen() {
		return (InviteToChatroom) screens.get(Screens.INVITE_TO_CHATROOM);
	}

	public void messageEvent(ChatMessage cm) {
		String msg = cm.getBody();
		if (msg != null && !msg.isEmpty()) {
			Conversations conversations = getConversationsScreen();
			Conversation conv = conversations.getFromId(cm.getThreadId());

			if (conv == null) {
				conv = chatEvent(cm, true);
			} else {
				conv.receiveMessage(cm.getFrom(), cm.getBody());
				if (!conversations.getActiveConversation().equals(cm.getThreadId())) {
					BMMessageEvent bme = new BMMessageEvent();
					bme.setConvId(cm.getThreadId());
					cml.newMessageReceived(bme);
				}
			}
		}
	}

	public void errorMessage(ErrorMessage em) {
		String msg = em.getBody();
		if (msg != null && !msg.isEmpty()) {
			Conversations conversations = getConversationsScreen();
			Conversation conv = conversations.getFromId(em.getThreadId());
			if (conv != null) {
				conv.errorMessage(msg);
			}

		}

	}

	public Conversation chatEvent(ChatMessage cm, final boolean markUnread) {

		final Conversation conv = new Conversation(cm.getThreadId(), cm.getFrom());
		conv.addParticipant(cm.getFrom());
		registerConversation(conv, true);

		LeftPanel fav = getLeftPanelScreen();
		fav.addNotInList(cm.getFrom());

		IInstantMessagingAsync im = new InstantMessagingGwtEndpoint(Ajax.TOKEN.getSessionId());

		im.getLastMessagesBetween(Ajax.getDefaultEmail(), cm.getFrom(), 50, new AsyncHandler<List<IMMessage>>() {

			@Override
			public void success(List<IMMessage> result) {
				conv.loadFromHistory(result, markUnread);
			}

			@Override
			public void failure(Throwable e) {
				// TODO Auto-generated method stub

			}
		});

		return conv;
	}

	public void rosterEntries(List<RosterItem> entries) {
		LeftPanel fav = getLeftPanelScreen();

		for (RosterItem p : entries) {
			fav.addFavoriteItem(p);
			fav.updatePresence(p);
		}

	}

	public void rosterEvent(RosterMessage rm) {
		LeftPanel fav = getLeftPanelScreen();

		if ("entries-added".equals(rm.getType())) {
			Push.entries();
		} else if ("entries-deleted".equals(rm.getType())) {
			for (int i = 0; i < rm.getEntries().length(); i++) {
				fav.removeFavoriteItem(rm.getEntries().get(i));
			}
		} else if ("presence".equals(rm.getType())) {
			RosterItem item = new RosterItem();
			item.user = rm.getUser();
			item.mode = rm.getMode();
			item.subs = rm.getSubs();
			item.status = rm.getPresenceStatus();
			item.subscriptionType = rm.getSubscriptionType();
			fav.updatePresence(item);
		}
	}

	public void createChat(String with) {
		JSONObject message = new JSONObject();
		message.put("userJID", new JSONString(with));
		Push.send("xmpp/session/" + Push.getSidFromPage() + ":chat", message);
	}

	public void setPresence(String mode, String status) {
		JSONObject message = new JSONObject();
		message.put("mode", new JSONString(mode));
		message.put("status", new JSONString(status));
		Push.send("xmpp/session/" + Push.getSidFromPage() + ":presence", message);
	}

	public void showRemoveFromFavoritesScreen(String jabberId) {
		RemoveFromFavorites rff = (RemoveFromFavorites) screens.get(Screens.REMOVE_FROM_FAVORITES);
		rff.setJabberId(jabberId);
	}

	public void showAddToFavoritesScreen(String jabberId) {
		AddToFavorites atf = (AddToFavorites) screens.get(Screens.ADD_TO_FAVORITES);
		atf.setJabberId(jabberId);
	}

	public void addBuddy(String jabberId) {
		JSONObject message = new JSONObject();
		message.put("user", new JSONString(jabberId));
		Push.send("xmpp/session/" + Push.getSidFromPage() + "/roster:add-buddy", message);
	}

	public void removeBuddy(String jabberId) {
		JSONObject message = new JSONObject();
		message.put("user", new JSONString(jabberId));
		Push.send("xmpp/session/" + Push.getSidFromPage() + "/roster:remove-buddy", message);
	}

	public void presenceEvent(PresenceMessage msg) {
		if ("subscribe".equals(msg.getAction())) {
			SubscriptionRequest sr = (SubscriptionRequest) screens.get(Screens.SUBSCRIPTION_REQUEST);
			sr.setJabberId(msg.getFrom());
		}
	}

	public void acceptSubscribe(String jabberId) {
		JSONObject message = new JSONObject();
		message.put("to", new JSONString(jabberId));
		Push.send("xmpp/session/" + Push.getSidFromPage() + ":accept-subscribe", message);

		//
		addBuddy(jabberId);
		markAllAsRead();
	}

	public void discardSubscribe(String jabberId) {
		JSONObject message = new JSONObject();
		message.put("to", new JSONString(jabberId));
		Push.send("xmpp/session/" + Push.getSidFromPage() + ":discard-subscribe", message);
		markAllAsRead();
	}

	public void createMuc(final Set<String> invitations) {
		JSONObject message = new JSONObject();

		// FIXME roomName
		String roomName = "groupchat-" + System.currentTimeMillis();

		message.put("name", new JSONString(roomName));

		// FIXME JID
		message.put("nickname", new JSONString(Ajax.getDefaultEmail()));

		Push.send("xmpp/muc/" + Push.getSidFromPage() + ":create", message, new MessageHandler<JavaScriptObject>() {

			@Override
			public void onMessage(JavaScriptObject message) {
				JSONObject jso = new JSONObject(message);
				double status = jso.get("status").isNumber().doubleValue();
				if (status == 0) {

					String room = jso.get("roomName").isString().stringValue();

					MucConversation groupChat = new MucConversation(room);
					registerConversation(groupChat, true);

					// Sends invitations
					sendMucInvitations(invitations, room);
				}

			}

		});
	}

	public void sendMucInvitations(final Set<String> invitations, String room) {

		Conversations conversations = getConversationsScreen();
		MucConversation conv = (MucConversation) conversations.getFromId(room);

		for (String jabberId : invitations) {
			JSONObject invite = new JSONObject();
			invite.put("latd", new JSONString(jabberId));
			invite.put("reason", new JSONString(IMConstants.INST.hasInvitedYouToGroupChat(Ajax.getDisplayName())));
			Push.send("xmpp/muc/" + Push.getSidFromPage() + "/" + room + ":invite", invite);

			String who = jabberId;
			RosterItem ri = RosterItemCache.getInstance().get(jabberId);
			if (ri != null) {
				who = ri.name;
			}

			conv.info(IMConstants.INST.youHaveInvited(who));
		}
	}

	public void mucMessageEvent(MucMessage mm) {
		String msg = mm.getMessage();
		if (msg != null && !msg.isEmpty()) {
			Conversations conversations = getConversationsScreen();
			MucConversation conv = (MucConversation) conversations.getFromId(mm.getThreadId());

			conv.receiveMessage(mm.getFrom(), mm.getMessage());

			if (!conversations.getActiveConversation().equals(mm.getThreadId())) {
				BMMessageEvent bme = new BMMessageEvent();
				bme.setConvId(mm.getThreadId());
				cml.newMessageReceived(bme);
			}
		}
	}

	public void mucJoinEvent(MucMessage mm) {
		Conversations conversations = getConversationsScreen();
		MucConversation conv = (MucConversation) conversations.getFromId(mm.getRoom());
		if (conv == null) {
			conv = new MucConversation(mm.getRoom());
			registerConversation(conv, true);
		}
		conv.addParticipant(mm.getParticipant());
	}

	public void mucLeaveEvent(MucMessage mm) {
		Conversations conversations = getConversationsScreen();
		MucConversation conv = (MucConversation) conversations.getFromId(mm.getRoom());
		conv.removeParticipant(mm.getParticipant());
	}

	public void mucInvitationEvent(MucMessage mm) {
		Conversations conversations = getConversationsScreen();
		Conversation conv = conversations.getFromId(mm.getInvitationRoom());
		if (conv == null) {
			NewInvitation ni = (NewInvitation) screens.get(Screens.NEW_INVITATION);
			ni.setInvitationEvent(mm);
			ni.center();
			ni.show();
		}
	}

	public void mucAcceptInvitation(String room) {
		JSONObject msg = new JSONObject();
		msg.put("room", new JSONString(room));
		msg.put("nickname", new JSONString(getJID()));
		Push.send("xmpp/muc/" + Push.getSidFromPage() + ":join", msg);
	}

	public void leaveMuc(String room) {
		JSONObject msg = new JSONObject();
		msg.put("room", new JSONString(room));
		msg.put("nickname", new JSONString(getJID()));
		Push.send("xmpp/muc/" + Push.getSidFromPage() + "/" + room + ":leave", msg);
	}

	public void updatePhoneStatus(PhoneMessage msg) {
		LeftPanel fav = getLeftPanelScreen();
		fav.updatePhoneStatus(msg.getLatd(), msg.getStatus());

	}

	public void markAllAsRead() {
		Push.send("xmpp/session/" + Push.getSidFromPage() + ":mark-all-as-read", new JSONObject());
	}

	public void loadUnreadMessage() {
		JSONObject msg = new JSONObject();
		msg.put("origin", new JSONString("im"));
		Push.send("xmpp/session/" + Push.getSidFromPage() + ":unread", msg);
	}

	public void loadPendingMuc() {
		JSONObject msg = new JSONObject();
		msg.put("origin", new JSONString("im"));
		Push.send("xmpp/muc/" + Push.getSidFromPage() + ":pending", msg);
	}

	public void open() {
		Overlay overlay = (Overlay) screens.get(Screens.OVERLAY);
		overlay.hide();
	}

	public void close() {
		Conversations conversations = getConversationsScreen();
		for (Conversation c : conversations.getConversations().values()) {
			if (c instanceof MucConversation) {
				conversations.unregister(c);
			}
		}

		Overlay overlay = (Overlay) screens.get(Screens.OVERLAY);
		overlay.show();

	}

	public void loadPresence() {
		Push.send("xmpp/session/" + Push.getSidFromPage() + ":ownPresence", new JSONObject());
	}

	public void ownPresenceEvent(PresenceMessage msg) {
		LeftPanel lp = (LeftPanel) screens.get(Screens.LEFT_PANEL);
		lp.updateOwnPresence(msg);
	}

	public void leaveMucs() {
		Push.send("xmpp/muc/" + Push.getSidFromPage() + ":close", new JSONObject());
	}

	public String getJID() {
		return Ajax.getDefaultEmail();
	}
}
