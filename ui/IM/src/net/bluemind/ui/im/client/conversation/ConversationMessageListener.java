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

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

public class ConversationMessageListener implements HasHandlers {
	private HandlerManager handlerManager;

	public ConversationMessageListener() {
		this.handlerManager = new HandlerManager(this);
	}

	@Override
	public void fireEvent(GwtEvent<?> event) {
		handlerManager.fireEvent(event);
	}

	public HandlerManager getHandlerManager() {
		return handlerManager;
	}

	public void setHandlerManager(HandlerManager handlerManager) {
		this.handlerManager = handlerManager;
	}

	public HandlerRegistration addMessageReceivedEventHandler(ConversationMessageHandler handler) {
		return handlerManager.addHandler(ReceiveMessageEvent.TYPE, handler);
	}

	public void newMessageReceived(BMMessageEvent message) {
		ReceiveMessageEvent event = new ReceiveMessageEvent(message);
		fireEvent(event);
	}

	public HandlerRegistration addMarkAsReadEventHandler(ConversationMessageHandler handler) {
		return handlerManager.addHandler(MarkAsReadEvent.TYPE, handler);
	}

	public void markAsRead(String jid) {
		MarkAsReadEvent event = new MarkAsReadEvent(jid);
		fireEvent(event);
	}
}
