/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.pop3.endpoint;

public class MailItemData {
	private long itemId;
	private String bodyMsgId;
	private int msgSize;

	public MailItemData(long id, String msgId, int size) {
		this.itemId = id;
		this.bodyMsgId = msgId;
		this.msgSize = size;
	}

	public long getItemId() {
		return itemId;
	}

	public void setItemId(long itemId) {
		this.itemId = itemId;
	}

	public String getBodyMsgId() {
		return bodyMsgId;
	}

	public void setBodyMsgId(String bodyMsgId) {
		this.bodyMsgId = bodyMsgId;
	}

	public int getMsgSize() {
		return msgSize;
	}

	public void setMsgSize(int msgSize) {
		this.msgSize = msgSize;
	}
}
