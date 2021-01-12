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

import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("serial")
public class PermissionDeniedException extends FilterException {

	public final List<String> recipients;

	public PermissionDeniedException(List<String> recipients) {
		super();
		this.recipients = recipients;
	}

	public String getErrorCode() {
		return "PERMISSION_DENIED";
	}

	public String toHeaderValue() {
		return recipients.stream().collect(Collectors.joining(","));
	}

	public static class MailboxInvitationDeniedException extends Exception {

		public final String mboxUid;

		public MailboxInvitationDeniedException(String mboxUid) {
			this.mboxUid = mboxUid;
		}

	}

	public static class CounterNotAllowedException extends Exception {

		public final String targetMailbox;

		public CounterNotAllowedException(String targetMailbox) {
			this.targetMailbox = targetMailbox;
		}

	}

}
