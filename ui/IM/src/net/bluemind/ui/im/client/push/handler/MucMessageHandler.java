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
package net.bluemind.ui.im.client.push.handler;

import net.bluemind.ui.im.client.IMCtrl;
import net.bluemind.ui.im.client.push.MessageHandler;
import net.bluemind.ui.im.client.push.message.MucMessage;

public class MucMessageHandler implements MessageHandler<MucMessage> {

	@Override
	public void onMessage(MucMessage msg) {
		String action = msg.getAction();
		if ("message".equals(action)) {
			IMCtrl.getInstance().mucMessageEvent(msg);
		} else if ("join".equals(action)) {
			IMCtrl.getInstance().mucJoinEvent(msg);
		} else if ("leave".equals(action)) {
			IMCtrl.getInstance().mucLeaveEvent(msg);
		} else if ("invite".equals(action)) {
			IMCtrl.getInstance().mucInvitationEvent(msg);
		}
	}
}
