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
package net.bluemind.milter;

import java.util.LinkedList;
import java.util.List;

public final class SmtpEnvelope {

	private List<SmtpAddress> mRecipients;
	private SmtpAddress sender;

	public SmtpEnvelope() {
		mRecipients = new LinkedList<SmtpAddress>();
	}

	public void setSender(SmtpAddress sender) {
		this.sender = sender;
	}

	public void addRecipient(SmtpAddress recipient) {
		mRecipients.add(recipient);
	}

	public List<SmtpAddress> getRecipients() {
		return mRecipients;
	}

	public SmtpAddress getSender() {
		return sender;
	}
}
