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

import java.util.HashMap;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiConstructor;

import net.bluemind.ui.common.client.ui.HorizontalTabLayoutPanel;
import net.bluemind.ui.im.client.IMCtrl;
import net.bluemind.ui.im.client.IScreen;
import net.bluemind.ui.im.client.RosterItemCache;
import net.bluemind.ui.im.client.leftpanel.RosterItem;

public class Conversations extends HorizontalTabLayoutPanel implements ConversationMessageHandler, IScreen {

	public interface ConversationsBundle extends ClientBundle {
		@Source("Conversations.css")
		ConversationsStyle getStyle();
	}

	public interface ConversationsStyle extends CssResource {
		public String watermark();
	}

	public static ConversationsStyle style;
	public static ConversationsBundle bundle;

	private HashMap<String, Conversation> conversations;
	private String activeConversation;

	public @UiConstructor Conversations(double barWidth, Unit barUnit) {
		super(barWidth, barUnit);

		bundle = GWT.create(ConversationsBundle.class);
		style = bundle.getStyle();
		style.ensureInjected();

		addStyleName(style.watermark());

		conversations = new HashMap<String, Conversation>();

		addSelectionHandler(new SelectionHandler<Integer>() {
			@Override
			public void onSelection(SelectionEvent<Integer> event) {
				final Conversation c = (Conversation) getWidget(event.getSelectedItem());
				Scheduler.get().scheduleDeferred(new ScheduledCommand() {
					@Override
					public void execute() {
						c.setFocus();
						if (activeConversation != null && !activeConversation.equals(c.getId())) {
							c.markAllAsRead();
						}
						activeConversation = c.getId();
					}
				});

			}
		});
	}

	/**
	 * @param c
	 * @param focus
	 */
	public void register(final Conversation c, boolean focus) {
		Conversation conv = conversations.get(c.getId());
		if (conv == null) {
			conversations.put(c.getId(), c);

			String name = c.getJabberId();
			RosterItem ri = RosterItemCache.getInstance().get(c.getJabberId());
			if (ri != null) {
				name = ri.name;
			}

			ConversationTab ct = new ConversationTab(name);
			ClickHandler ch = new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					event.preventDefault();
					IMCtrl.getInstance().unregisterConversation(c);
				}
			};
			ct.setCloseBtnAction(ch);
			add(c, ct);
			conv = c;
		}
		if (focus) {
			selectTab(conv);
		}
	}

	/**
	 * @param c
	 */
	public void unregister(Conversation c) {
		conversations.remove(c.getId());
		remove(c);
	}

	public Conversation getFromId(String id) {
		return conversations.get(id);
	}

	@Override
	public void onMessageReceived(ReceiveMessageEvent event) {
		BMMessageEvent bme = event.getBMMessageEvent();
		Conversation conv = conversations.get(bme.getConvId());
		if (conv != null) {
			ConversationTab tab = (ConversationTab) getTabWidget(conv);
			tab.setHighlight();
		}
	}

	@Override
	public void onMarkAsRead(MarkAsReadEvent event) {
		String jid = event.getJid();
		Conversation conv = conversations.get(jid);
		GWT.log("mark as read: " + jid);
		if (conv != null) {
			ConversationTab tab = (ConversationTab) getTabWidget(conv);
			tab.setDownlight();
		}
	}

	public HashMap<String, Conversation> getConversations() {
		return conversations;
	}

	public void setConversations(HashMap<String, Conversation> conversations) {
		this.conversations = conversations;
	}

	public String getActiveConversation() {
		return activeConversation;
	}

	public void setActiveConversation(String activeConversation) {
		this.activeConversation = activeConversation;
	}
}
