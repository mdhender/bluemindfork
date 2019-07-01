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

import net.bluemind.ui.im.client.IMConstants;
import net.bluemind.ui.im.client.IMCtrl;
import net.bluemind.ui.im.client.conversation.InvitationList;

public class InviteToChatroom extends InvitationList {

	private String room;

	public InviteToChatroom() {
		super();
		setPlaceHolder(IMConstants.INST.inviteToGroupChat());
		submit.setText(IMConstants.INST.inviteButton());
		cancel.setText(IMConstants.INST.cancelButton());
	}

	@Override
	public void submit() {
		if (!invitations.isEmpty() && room != null) {
			IMCtrl.getInstance().sendMucInvitations(invitations.keySet(), room);
			hide();
		}
	}

	public void setRoom(String room) {
		this.room = room;
	}

}
