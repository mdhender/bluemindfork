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
package net.bluemind.ui.im.client.chatroom;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.ui.im.client.IMConstants;
import net.bluemind.ui.im.client.IMCtrl;
import net.bluemind.ui.im.client.IScreen;
import net.bluemind.ui.im.client.RosterItemCache;
import net.bluemind.ui.im.client.leftpanel.RosterItem;
import net.bluemind.ui.im.client.push.message.MucMessage;

public class NewInvitation extends DialogBox implements IScreen {
	private static final Binder binder = GWT.create(Binder.class);

	interface Binder extends UiBinder<Widget, NewInvitation> {
	}

	@UiField
	Label reason; // u r reason

	@UiField
	Button join;

	@UiField
	Button decline;

	private String room;

	public NewInvitation() {
		setWidget(binder.createAndBindUi(this));
		setGlassEnabled(true);
		setGlassStyleName("bmOverlay");
		setModal(true);

		join.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				try {
					IMCtrl.getInstance().mucAcceptInvitation(room);
					IMCtrl.getInstance().markAllAsRead();
				} catch (Exception e) {
					GWT.log(e.getMessage(), e);
				}
				hide();
			}
		});

		decline.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				IMCtrl.getInstance().markAllAsRead();
				hide();
			}
		});
	}

	public void setInvitationEvent(MucMessage mm) {
		this.room = mm.getInvitationRoom();
		String reason = mm.getInvitationReason();
		if (reason == null || reason.trim().isEmpty()) {
			String inviter = mm.getInvitationInviter();
			RosterItem ri = RosterItemCache.getInstance().get(inviter);
			if (ri != null) {
				inviter = ri.name;
			}
			reason = IMConstants.INST.hasInvitedYouToGroupChat(inviter);
		}
		this.reason.setText(reason);
	}
}
