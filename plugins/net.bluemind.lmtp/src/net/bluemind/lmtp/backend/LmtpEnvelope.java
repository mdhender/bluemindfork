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
package net.bluemind.lmtp.backend;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public final class LmtpEnvelope {

	private List<LmtpAddress> mRecipients;
	private LmtpAddress sender;
	private final String id;

	public LmtpEnvelope() {
		mRecipients = new LinkedList<>();
		id = UUID.randomUUID().toString();
	}

	public boolean hasSender() {
		return sender != null;
	}

	public boolean hasRecipients() {
		return !mRecipients.isEmpty();
	}

	public void setSender(LmtpAddress sender) {
		this.sender = sender;
	}

	public void addRecipient(LmtpAddress recipient) {
		mRecipients.add(recipient);
	}

	public List<LmtpAddress> getRecipients() {
		return mRecipients;
	}

	public LmtpAddress getSender() {
		return sender;
	}

	public String getId() {
		return id;
	}

}
