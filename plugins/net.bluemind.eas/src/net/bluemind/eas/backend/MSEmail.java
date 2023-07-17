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
package net.bluemind.eas.backend;

import org.apache.james.mime4j.message.MessageImpl;

import com.google.common.io.ByteSource;

import net.bluemind.eas.dto.type.ItemDataType;

public class MSEmail implements IApplicationData {

	@Override
	public ItemDataType getType() {
		return ItemDataType.EMAIL;
	}

	private Boolean read;
	private Boolean starred;
	private MessageImpl message;
	private ByteSource mimeContent;

	public MSEmail() {
		read = false;
		starred = false;
	}

	public Boolean isRead() {
		return read;
	}

	public void setRead(Boolean read) {
		this.read = read;
	}

	public Boolean isStarred() {
		return starred;
	}

	public void setStarred(Boolean starred) {
		this.starred = starred;
	}

	public ByteSource getMimeContent() {
		return mimeContent;
	}

	public void setMimeContent(ByteSource content) {
		this.mimeContent = content;
	}

	public void setMessage(MessageImpl message) {
		this.message = message;
	}

	public MessageImpl getMessage() {
		return message;
	}

}
