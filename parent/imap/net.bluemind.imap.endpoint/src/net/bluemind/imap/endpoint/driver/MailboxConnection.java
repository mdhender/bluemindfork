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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.imap.endpoint.driver;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.streams.WriteStream;
import net.bluemind.mailbox.api.MailboxQuota;

public interface MailboxConnection {

	SelectedFolder select(String fName);

	List<ListNode> list(String reference, String mailboxPattern);

	CompletableFuture<Void> fetch(SelectedFolder selected, String idset, List<MailPart> fetchSpec,
			WriteStream<FetchedItem> output);

	MailboxQuota quota();

	void idleMonitor(SelectedFolder selected, WriteStream<IdleToken> ctx);

	void notIdle();

}
