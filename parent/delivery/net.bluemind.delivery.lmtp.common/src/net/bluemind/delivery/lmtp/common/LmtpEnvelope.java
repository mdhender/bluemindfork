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
package net.bluemind.delivery.lmtp.common;

import java.util.List;

public class LmtpEnvelope {

	private final String senderEmail;
	private List<ResolvedBox> recipients;

	public LmtpEnvelope(String senderEmail, List<ResolvedBox> recipients) {
		this.senderEmail = senderEmail;
		this.recipients = recipients;
	}

	public String getSenderEmail() {
		return senderEmail;
	}

	public List<ResolvedBox> getRecipients() {
		return recipients;
	}

}
