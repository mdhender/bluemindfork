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
package net.bluemind.ui.im.client.conversation;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.im.api.IMMessage;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.im.client.IMConstants;
import net.bluemind.ui.im.client.IMCtrl;
import net.bluemind.ui.im.client.RosterItemCache;
import net.bluemind.ui.im.client.chatroom.RoomOccupant;
import net.bluemind.ui.im.client.leftpanel.RosterItem;
import net.bluemind.ui.im.client.push.Push;

public class Conversation extends Composite implements HasClickHandlers {

	private static ConversationUiBinder uiBinder = GWT.create(ConversationUiBinder.class);

	interface ConversationUiBinder extends UiBinder<DockLayoutPanel, Conversation> {
	}

	public interface ConversationBundle extends ClientBundle {
		@Source("Conversation.css")
		ConversationStyle getStyle();
	}

	public interface ConversationStyle extends CssResource {
		public String composer();

		public String toolbar();

		public String error();

		public String info();

		public String me();

		public String msgContainer();

		public String conversation();

		public String invite();

		public String disabled();

		public String notifyNewMessage();

		public String roomOccupants();
	}

	@UiField
	DockLayoutPanel dlp;

	@UiField
	ScrollPanel container;

	@UiField
	FlowPanel conversationPanel;

	@UiField
	FlowPanel composer;

	@UiField
	TextArea input;

	@UiField
	FlowPanel roomOccupants;

	@UiField
	Label notifyNewMessage;

	@UiField
	FlowPanel toolbar;

	private static final int NEW_MESSAGE_DELAY = 60000;

	private DateTimeFormat dateFormat;
	private DateTimeFormat timeFormat;
	private DateTimeFormat dateTimeFormat;
	private Message lastSent;
	private Message lastReceived;
	private String lastFrom;
	private List<Message> unread;
	protected static ConversationStyle style;
	private static ConversationBundle bundle;
	protected String jabberId;

	protected String id;

	public Conversation(String threadId, String jabberId) {
		initWidget(uiBinder.createAndBindUi(this));

		notifyNewMessage.setVisible(false);

		dateFormat = DateTimeFormat.getFormat("yyyy-MM-dd");
		timeFormat = DateTimeFormat.getFormat("HH:mm");
		dateTimeFormat = DateTimeFormat.getFormat("EEE dd MMM, HH:mm");

		bundle = GWT.create(ConversationBundle.class);
		style = bundle.getStyle();
		style.ensureInjected();

		id = threadId;
		this.jabberId = jabberId;

		setStyleName(style.conversation());
		roomOccupants.setStyleName(style.roomOccupants());
		container.setStyleName(style.msgContainer());

		composer.setStyleName(style.composer());
		composer.getElement().getStyle().setPosition(Position.RELATIVE);
		toolbar.setStyleName(style.toolbar());
		input.getElement().setAttribute("placeholder", IMConstants.INST.sendMessagePlaceholder());

		unread = new LinkedList<Message>();

		input.addKeyPressHandler(new KeyPressHandler() {

			@Override
			public void onKeyPress(KeyPressEvent event) {
				if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER && !event.isShiftKeyDown()) {
					event.preventDefault();
					if (input.getValue() != null && !input.getValue().isEmpty()) {
						IMCtrl.getInstance().sendMessage(id, input.getValue());
						input.setValue(null);
					}
				}
			}
		});

		addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				markAllAsRead();
				if (!isTextSelected()) {
					setFocus();
				}
			}
		});

		notifyNewMessage.setStyleName(style.notifyNewMessage());
		notifyNewMessage.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				event.stopPropagation();
				container.scrollToBottom();
			}
		});

		container.addScrollHandler(new ScrollHandler() {

			@Override
			public void onScroll(ScrollEvent event) {
				if (container.getMaximumVerticalScrollPosition() == container.getVerticalScrollPosition()) {
					notifyNewMessage.setVisible(false);
				}
			}
		});

		Window.addResizeHandler(new ResizeHandler() {

			@Override
			public void onResize(ResizeEvent event) {
				Conversations conv = IMCtrl.getInstance().getConversationsScreen();
				if (conv.getActiveConversation().equals(id)) {
					computeLayoutAndScrollToBottom();
				}
			}
		});

		inviteButton();
	}

	/**
	 * 
	 */
	private native boolean isTextSelected() /*-{
		var ret = false;
		try {
			ret = ($wnd.getSelection().toString() != '');
		} catch (e) {
		}

		return ret;
	}-*/;

	public String getId() {
		return id;
	}

	public String getJabberId() {
		return jabberId;
	}

	public List<Message> getUnread() {
		return unread;
	}

	public void markAllAsRead() {
		if (unread.size() > 0) {
			for (Message m : unread) {
				m.markAsRead();
			}
			unread.clear();
			IMCtrl.getInstance().markAsRead(id);
		}
	}

	public void setFocus() {
		input.setFocus(true);
		computeLayoutAndScrollToBottom();
	}

	@Override
	public HandlerRegistration addClickHandler(ClickHandler handler) {
		return addDomHandler(handler, ClickEvent.getType());
	}

	/**
	 * @param scroll
	 * @param msg
	 */
	private void scrollOrNotify(boolean scroll, String msg) {
		if (scroll) {
			container.scrollToBottom();
		} else {
			if (msg.length() >= 50) {
				msg = msg.substring(0, 50) + " ...";
			}
			notifyNewMessage.setText("↓ " + msg + " ↓");
		}
		notifyNewMessage.setVisible(!scroll);
	}

	/**
	 * Computes layout after resizing or moving from one tab to another and
	 * forces active conversation to scroll bottom
	 */
	private void computeLayoutAndScrollToBottom() {
		dlp.forceLayout();
		Scheduler.get().scheduleDeferred(new ScheduledCommand() {

			@Override
			public void execute() {
				container.scrollToBottom();
			}
		});
	}

	public void addParticipant(String jabberId) {
		RoomOccupant ro = new RoomOccupant(jabberId);
		ro.addStyleName("c1"); // orange
		roomOccupants.add(ro);
	}

	protected void inviteButton() {
		Label invite = new Label();
		invite.setSize("12px", "12px");

		invite.setStyleName("fa fa-lg fa-plus-square-o add-invitee-sign");
		invite.setTitle(IMConstants.INST.inviteToGroupChat());

		invite.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				CreateConversation cr = IMCtrl.getInstance().getCreateConversationsScreen();
				Widget source = (Widget) event.getSource();
				int left = source.getAbsoluteLeft() - 40;
				int top = source.getAbsoluteTop() + 24;
				cr.setPopupPosition(left, top);
				cr.show();
				cr.addToInvitees(jabberId);
			}
		});

		roomOccupants.add(invite);
	}

	public void receiveMessage(String from, String message) {
		boolean scroll = false;

		if (container.getMaximumVerticalScrollPosition() == container.getVerticalScrollPosition()) {
			scroll = true;
		}

		appendMessage(from, message, new Date());

		if (!getJID().equals(from)) {
			lastReceived.setUnread();
			unread.add(lastReceived);
			scrollOrNotify(scroll, from + ": " + message);
		} else {
			container.scrollToBottom();
		}
	}

	private String getJID() {
		return Ajax.getDefaultEmail();
	}

	/**
	 * @param result
	 * @param markUnread
	 * @param cm
	 */
	public void loadFromHistory(List<IMMessage> result, boolean markUnread) {
		if (result != null) {
			for (IMMessage m : result) {
				appendMessage(m.from, m.body, m.timestamp);
			}
			container.scrollToBottom();
			if (markUnread) {
				lastReceived.setUnread();
				unread.add(lastReceived);
			}
		}
	}

	/**
	 * @param from
	 * @param msg
	 * @param d
	 * @param fromHistory
	 */
	private void appendMessage(String from, String msg, Date d) {

		if (msg.startsWith("/me ")) {
			me(from, msg);
			return;
		}

		Message m = new Message();
		m.setTimestamp(d);
		m.appendMessage(msg);
		Date now = new Date();
		if (dateFormat.format(now).equals(dateFormat.format(d))) {
			m.setDate(timeFormat.format(d));
		} else {
			m.setDate(dateTimeFormat.format(d));
		}

		RosterItem ri = RosterItemCache.getInstance().get(from);
		if (ri != null) {
			if (ri.photo != null) {
				StringBuilder dataUrl = new StringBuilder();
				dataUrl.append("data:image/jpeg;base64,");
				dataUrl.append(ri.photo);
				m.setPicture(dataUrl.toString());
			}
			m.setHeaderText(ri.name);
		} else {
			m.setHeaderText(from);
		}

		if ((lastFrom == null && lastReceived == null)
				|| m.getTimestamp().getTime() - lastReceived.getTimestamp().getTime() > NEW_MESSAGE_DELAY
				|| !lastFrom.equals(from)) {
			conversationPanel.add(m);
			lastReceived = m;
		} else {
			lastReceived.appendMessage(msg);
		}

		lastFrom = from;
		lastReceived.setColor(getColor(from));
		lastSent = null;
	}

	private void me(String from, String msg) {
		boolean scroll = false;
		if (container.getMaximumVerticalScrollPosition() == container.getVerticalScrollPosition()) {
			scroll = true;
		}

		RosterItem item = RosterItemCache.getInstance().get(from);
		if (item != null) {
			from = item.name;
		}

		msg = msg.replace("/me ", from + " ");
		Label l = new Label(msg);
		l.setStyleName(style.me());
		conversationPanel.add(l);
		scrollOrNotify(scroll, msg);
		lastSent = null;
		lastReceived = null;
		lastFrom = null;
	}

	protected int getColor(String from) {
		int c = 0; // blue
		if (!from.equals(getJID())) {
			c = 1; // orange
		}
		return c;
	}

	/**
	 * @param msg
	 */
	public void sendMessage(String msg) {
		JSONObject message = new JSONObject();
		message.put("message", new JSONString(msg));
		Push.send("xmpp/session/" + Push.getSidFromPage() + "/chat/" + id + ":message", message);
		container.scrollToBottom();
		markAllAsRead();
	}

	public void info(String msg) {
		boolean scroll = false;
		if (container.getMaximumVerticalScrollPosition() == container.getVerticalScrollPosition()) {
			scroll = true;
		}
		Label l = new Label(msg);
		l.setStyleName(style.info());
		conversationPanel.add(l);
		scrollOrNotify(scroll, msg);
		lastSent = null;
		lastReceived = null;
		lastFrom = null;

	}

	public void errorMessage(String msg) {
		boolean scroll = false;
		if (container.getMaximumVerticalScrollPosition() == container.getVerticalScrollPosition()) {
			scroll = true;
		}

		Label l = new Label(msg);
		l.setStyleName(style.error());
		conversationPanel.add(l);
		scrollOrNotify(scroll, msg);
		lastSent = null;
		lastReceived = null;
		lastFrom = null;

	}
}
