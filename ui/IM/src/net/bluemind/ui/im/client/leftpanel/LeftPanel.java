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
package net.bluemind.ui.im.client.leftpanel;

import java.util.HashMap;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.im.client.IMConstants;
import net.bluemind.ui.im.client.IMCtrl;
import net.bluemind.ui.im.client.IScreen;
import net.bluemind.ui.im.client.conversation.CreateConversation;
import net.bluemind.ui.im.client.push.message.PresenceMessage;

public class LeftPanel extends Composite implements IScreen {

	private static LeftPanelUiBinder uiBinder = GWT.create(LeftPanelUiBinder.class);

	interface LeftPanelUiBinder extends UiBinder<DockLayoutPanel, LeftPanel> {
	}

	public interface RosterBundle extends ClientBundle {
		@Source("LeftPanel.css")
		RosterStyle getStyle();
	}

	public interface RosterStyle extends CssResource {
		public String panel();

		public String actions();

		public String connectionErr();
	}

	public static RosterStyle style;
	public static RosterBundle bundle;
	private HashMap<String, Entry> favs;
	private HashMap<String, Entry> notInListItems;
	private IMCtrl ctrl;

	@UiField
	ContactList favorite;

	@UiField
	ContactList notInList;

	@UiField
	SimplePanel actions;

	@UiField
	Header header;

	@UiField
	Label errMsg;

	@UiField
	DisclosurePanel favoriteContainer;

	@UiField
	DisclosurePanel notInListContainer;

	public LeftPanel() {
		initWidget(uiBinder.createAndBindUi(this));
		setHeight("100%");

		notInListContainer.setVisible(false);
		notInListContainer.setOpen(true);

		favoriteContainer.setOpen(true);

		ctrl = IMCtrl.getInstance();

		bundle = GWT.create(RosterBundle.class);
		style = bundle.getStyle();
		style.ensureInjected();

		favs = new HashMap<String, Entry>();
		notInListItems = new HashMap<String, Entry>();

		setStyleName(style.panel());

		actions.setStyleName(style.actions());
		errMsg.setStyleName(style.connectionErr());

		Button btn = new Button(IMConstants.INST.newConversationButton());
		btn.setStyleName("button primary");
		btn.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				CreateConversation cr = ctrl.getCreateConversationsScreen();
				Widget source = (Widget) event.getSource();
				int left = source.getAbsoluteLeft();
				int top = source.getAbsoluteTop() + 30;
				cr.setPopupPosition(left, top);
				cr.show();
			}
		});
		actions.add(btn);
	}

	public void addFavoriteItem(RosterItem ri) {
		if (!favs.containsKey(ri.user)) {
			Entry e = new Entry(ri);
			favs.put(ri.user, e);
			favorite.add(e);
			removeFromNotInList(ri.user);
		}
	}

	public void removeFavoriteItem(String jabberId) {
		Entry e = favs.get(jabberId);
		if (e != null) {
			favorite.remove(e);
			favs.remove(jabberId);
		}
	}

	public void updatePresence(RosterItem p) {
		Entry e = favs.get(p.user);
		if (e != null) {
			e.updatePresence(p);
		}
	}

	public void updateOwnPresence(PresenceMessage p) {
		header.updatePresence(p);
	}

	public void addNotInList(String jabberId) {
		if (!favs.containsKey(jabberId) && !notInListItems.containsKey(jabberId)) {
			RosterItem ri = new RosterItem();
			ri.user = jabberId;
			ri.name = jabberId;
			Entry e = new Entry(ri);
			e.setSubscribeAction();
			notInListItems.put(jabberId, e);
			notInList.add(e);
			notInListContainer.setVisible(true);
		}
	}

	public void removeFromNotInList(String jabberId) {
		Entry e = notInListItems.get(jabberId);
		if (e != null) {
			notInList.remove(e);
			notInListItems.remove(jabberId);
		}

		if (notInListItems.size() == 0) {
			notInListContainer.setVisible(false);
		}
	}

	public void updatePhoneStatus(String jid, String status) {
		if (jid.equals(Ajax.getDefaultEmail())) {
			header.updatePhoneStatus(status);
		} else {
			for (Entry entry : favs.values()) {
				if (entry.getLatd().equals(jid)) {
					entry.updatePhoneStatus(status);
				}
			}
		}

	}

}
