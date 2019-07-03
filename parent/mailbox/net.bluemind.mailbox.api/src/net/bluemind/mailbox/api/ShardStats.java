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
package net.bluemind.mailbox.api;

import java.util.List;
import java.util.Set;

import javax.validation.constraints.NotNull;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class ShardStats {
	@NotNull
	public String indexName;

	@NotNull
	public Set<String> mailboxes;

	@NotNull
	public List<MailboxStats> topMailbox;

	public long docCount;

	@NotNull
	public State state;

	/**
	 * index size in byte
	 */
	public long size;

	@BMApi(version = "3")
	public enum State {
		OK, HALF_FULL, FULL, SPLIT_NEEDED
	}

	@BMApi(version = "3")
	public static class MailboxStats {
		public String mailboxUid;
		public long docCount;
	}
}
