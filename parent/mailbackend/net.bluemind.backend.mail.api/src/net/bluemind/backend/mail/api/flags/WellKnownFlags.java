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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mail.api.flags;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;

import net.bluemind.backend.mail.api.flags.MailboxItemFlag.System;

public class WellKnownFlags {

	private static final Map<String, MailboxItemFlag> known = buildSystemList();

	private WellKnownFlags() {
	}

	private static Map<String, MailboxItemFlag> buildSystemList() {
		return ImmutableMap.<String, MailboxItemFlag>builder()//
				.put(System.Answered.value().flag, System.Answered.value())//
				.put(System.Flagged.value().flag, System.Flagged.value())//
				.put(System.Deleted.value().flag, System.Deleted.value())//
				.put(System.Draft.value().flag, System.Draft.value())//
				.put(System.Seen.value().flag, System.Seen.value())//
				.build();
	}

	public static MailboxItemFlag resolve(String s) {
		return Optional.ofNullable(known.get(s)).orElse(new MailboxItemFlag(s, 0));
	}

}
