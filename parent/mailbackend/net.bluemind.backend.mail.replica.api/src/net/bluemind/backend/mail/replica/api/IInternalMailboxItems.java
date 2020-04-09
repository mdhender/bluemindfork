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
package net.bluemind.backend.mail.replica.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.core.container.model.ItemIdentifier;

public interface IInternalMailboxItems extends IMailboxItems {

	List<ItemIdentifier> multiCreate(List<MailboxItem> items);

	String imapFolder();

	public interface ImapClient {
		boolean select(String mbox);

		Map<Integer, Integer> uidCopy(Collection<Integer> uids, String destMailbox);
	}

	public interface ImapCommandRunner {
		void withClient(Consumer<ImapClient> scCons);
	}

	ImapCommandRunner imapExecutor();

}
