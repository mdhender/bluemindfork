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

import jakarta.validation.constraints.NotNull;
import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class ShardStats extends SimpleShardStats {

	@BMApi(version = "3")
	public enum State {
		OK, HALF_FULL, FULL, SPLIT_NEEDED
	}

	@BMApi(version = "3")
	public static class MailboxStats {
		public String mailboxUid;
		public long docCount;
	}

	@NotNull
	public List<MailboxStats> topMailbox;

	@NotNull
	public State state;

}
