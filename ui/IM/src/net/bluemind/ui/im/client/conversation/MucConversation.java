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
import java.util.Map;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.im.client.IMConstants;
import net.bluemind.ui.im.client.IMCtrl;
import net.bluemind.ui.im.client.RosterItemCache;
import net.bluemind.ui.im.client.chatroom.InviteToChatroom;
import net.bluemind.ui.im.client.chatroom.RoomOccupant;
import net.bluemind.ui.im.client.leftpanel.RosterItem;
import net.bluemind.ui.im.client.push.Push;

public class MucConversation extends Conversation {

	private Map<String, RoomOccupant> occupants;
	private Map<String, Integer> colors;

	public MucConversation(final String roomName) {
		super(roomName, roomName);

		occupants = new HashMap<String, RoomOccupant>();
		colors = new HashMap<String, Integer>();

		addParticipant(Ajax.getDefaultEmail());

		// send history
		Label fwd = new Label();
		fwd.setStyleName("fa fa-paper-plane");
		fwd.setTitle(IMConstants.INST.sendGroupChatHistory());
		fwd.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				event.stopPropagation();
				IMCtrl.getInstance().showSendHistoryDialog(roomName);
			}
		});

		toolbar.add(fwd);

	}

	@Override
	public void sendMessage(String msg) {
		JSONObject message = new JSONObject();
		message.put("message", new JSONString(msg));
		Push.send("xmpp/muc/" + Push.getSidFromPage() + "/" + id + ":message", message);
		container.scrollToBottom();
		markAllAsRead();
	}

	@Override
	public void addParticipant(final String jabberId) {
		final RoomOccupant ro = new RoomOccupant(jabberId);

		if (!occupants.containsKey(jabberId)) {
			occupants.put(jabberId, ro);

			if (!colors.containsKey(jabberId)) {
				int color = colors.size() % 18;
				colors.put(jabberId, color);
				ro.addStyleName("c" + color);
			} else {
				ro.addStyleName("c" + colors.get(jabberId));
			}

			roomOccupants.add(ro);

			String who = jabberId;
			RosterItem ri = RosterItemCache.getInstance().get(jabberId);
			if (ri != null) {
				who = ri.name;
			}

			info(IMConstants.INST.hasJoinGroupChat(who));
		}
	}

	public void removeParticipant(String participant) {
		RoomOccupant ro = occupants.get(participant);
		roomOccupants.remove(ro);
		occupants.remove(participant);
		String who = participant;
		RosterItem ri = RosterItemCache.getInstance().get(participant);
		if (ri != null) {
			who = ri.name;
		}
		info(IMConstants.INST.hasLeftGroupChat(who));
	}

	@Override
	protected void inviteButton() {
		Label invite = new Label();
		invite.setSize("12px", "12px");
		invite.setStyleName("fa fa-lg fa-plus-square-o add-invitee-sign");
		invite.setTitle(IMConstants.INST.inviteToGroupChat());

		invite.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				InviteToChatroom itc = IMCtrl.getInstance().getInviteToChatroomScreen();
				itc.setRoom(id);

				Widget source = (Widget) event.getSource();
				int left = source.getAbsoluteLeft();
				int top = source.getAbsoluteTop() + 24;
				itc.setPopupPosition(left, top);
				itc.show();
			}
		});
		roomOccupants.add(invite);
	}

	@Override
	protected int getColor(String from) {
		return colors.get(from);
	}
}
