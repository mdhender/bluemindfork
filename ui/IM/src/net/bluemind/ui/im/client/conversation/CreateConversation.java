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

import net.bluemind.ui.im.client.IMConstants;
import net.bluemind.ui.im.client.IMCtrl;

public class CreateConversation extends InvitationList {

	public CreateConversation() {
		super();
		setPlaceHolder(IMConstants.INST.createConversationPlaceholder());
		submit.setText(IMConstants.INST.createConversationButton());
		cancel.setText(IMConstants.INST.cancelButton());
	}

	@Override
	public void submit() {
		if (!invitations.isEmpty()) {
			if (invitations.size() == 1) {
				for (String k : invitations.keySet()) {
					IMCtrl.getInstance().createChat(invitations.get(k).getJabberId());
				}
			} else {
				IMCtrl.getInstance().createMuc(invitations.keySet());

			}
			hide();
		}
	}
}
