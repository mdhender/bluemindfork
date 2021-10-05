/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.backend.mail.parsing;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;

import net.bluemind.backend.mail.replica.api.MailApiHeaders;

public class InternalHeaders implements HeaderList {

	// copied from UidFetchCommand
	private static final Set<String> fromSummaryClass = Sets.newHashSet("DATE", "FROM", "TO", "CC", "SUBJECT",
			"CONTENT-TYPE", "REPLY-TO", "MAIL-REPLY-TO", "MAIL-FOLLOWUP-TO", "LIST-POST", "DISPOSITION-NOTIFICATION-TO",
			"X-PRIORITY", "X-BM_HSM_ID", "X-BM_HSM_DATETIME", "X-BM-EVENT", "X-BM-EVENT-CANCELED",
			"X-BM-RESOURCEBOOKING", "X-BM-FOLDERSHARING", "X-ASTERISK-CALLERID", "X-BM-EVENT-COUNTERED");

	private static final Set<String> fromMailApi = Sets.newHashSet(MailApiHeaders.ALL);

	private static final Set<String> toAdd = Sets.newHashSet("IN-REPLY-TO", "REFERENCES");

	@Override
	public Set<String> getWhiteList() {
		Set<String> whitelist = new HashSet<>();
		whitelist.addAll(fromSummaryClass);
		whitelist.addAll(fromMailApi);
		whitelist.addAll(toAdd);
		return whitelist;
	}

}
