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

import java.util.HashMap;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.SimplePanel;

import net.bluemind.ui.im.client.chatroom.InviteToChatroom;
import net.bluemind.ui.im.client.chatroom.NewInvitation;
import net.bluemind.ui.im.client.chatroom.SendHistory;
import net.bluemind.ui.im.client.conversation.Conversation;
import net.bluemind.ui.im.client.conversation.ConversationMessageHandler;
import net.bluemind.ui.im.client.conversation.Conversations;
import net.bluemind.ui.im.client.conversation.CreateConversation;
import net.bluemind.ui.im.client.conversation.MarkAsReadEvent;
import net.bluemind.ui.im.client.conversation.ReceiveMessageEvent;
import net.bluemind.ui.im.client.leftpanel.LeftPanel;
import net.bluemind.ui.im.client.subscription.AddToFavorites;
import net.bluemind.ui.im.client.subscription.RemoveFromFavorites;
import net.bluemind.ui.im.client.subscription.SubscriptionRequest;
import net.bluemind.ui.im.client.viewport.Viewport;

public class RootScreen extends Composite implements ConversationMessageHandler {

	private static RootScreenUiBinder uiBinder = GWT.create(RootScreenUiBinder.class);

	private static IMCtrl ctrl;
	private Storage storage;

	interface RootScreenUiBinder extends UiBinder<DockLayoutPanel, RootScreen> {
	}

	@UiField
	LeftPanel leftPanel;

	@UiField
	SimplePanel conversationsContainer;

	private static Timer titleTimer;
	private Conversations conversations;

	public RootScreen() {
		initWidget(uiBinder.createAndBindUi(this));

		ctrl = IMCtrl.getInstance();

		ctrl.registerScreen(Screens.LEFT_PANEL, leftPanel);

		conversations = new Conversations(36, Unit.PX);
		conversationsContainer.add(conversations);
		ctrl.registerScreen(Screens.CONVERSATIONS, conversations);
		ctrl.registerScreen(Screens.CREATE_CONVERSATION, new CreateConversation());
		ctrl.registerScreen(Screens.ADD_TO_FAVORITES, new AddToFavorites());
		ctrl.registerScreen(Screens.REMOVE_FROM_FAVORITES, new RemoveFromFavorites());
		ctrl.registerScreen(Screens.INVITE_TO_CHATROOM, new InviteToChatroom());
		ctrl.registerScreen(Screens.SEND_HISTORY, new SendHistory());
		ctrl.registerScreen(Screens.SUBSCRIPTION_REQUEST, new SubscriptionRequest());
		ctrl.registerScreen(Screens.NEW_INVITATION, new NewInvitation());

		ctrl.registerScreen(Screens.OVERLAY, new Overlay());

		ctrl.addMessageReceivedEventHandler(conversations);
		ctrl.addMarkAsReadEventHandler(conversations);

		ctrl.addMessageReceivedEventHandler(this);
		ctrl.addMarkAsReadEventHandler(this);

		titleTimer = new Timer() {
			@Override
			public void run() {
				if ("BlueMind IM".equals(Window.getTitle())) {
					Window.setTitle(IMConstants.INST.unreadMessages());
				} else {
					Window.setTitle("BlueMind IM");
				}
			}
		};

		storage = Storage.getLocalStorageIfSupported();

		// Viewport stuff
		updateViewport();

		Viewport.get().addFocusHandler(new FocusHandler() {
			@Override
			public void onFocus(FocusEvent event) {
				updateViewport();
			}
		});

		Viewport.get().addBlurHandler(new BlurHandler() {
			@Override
			public void onBlur(BlurEvent event) {
				updateViewport();
			}
		});

		Window.addCloseHandler(new CloseHandler<Window>() {

			@Override
			public void onClose(CloseEvent<Window> event) {
				IMCtrl.getInstance().leaveMucs();
				storage.removeItem("bm-im-focus");
			}
		});

	}

	private void updateViewport() {
		if (Viewport.hasFocus()) {
			storage.setItem("bm-im-focus", "true");
		} else {
			storage.removeItem("bm-im-focus");
		}
	}

	/**
	 * 
	 */
	private static void cancelTitleTimer() {
		titleTimer.cancel();
		Window.setTitle("BlueMind IM");

	}

	@Override
	public void onMessageReceived(ReceiveMessageEvent event) {
		titleTimer.cancel();
		titleTimer.scheduleRepeating(750);
	}

	@Override
	public void onMarkAsRead(MarkAsReadEvent event) {
		HashMap<String, Conversation> convs = conversations.getConversations();
		int unread = 0;
		for (Conversation c : convs.values()) {
			unread += c.getUnread().size();
		}
		if (unread == 0) {
			cancelTitleTimer();
			IMCtrl.getInstance().markAllAsRead();
		}
	}

}
