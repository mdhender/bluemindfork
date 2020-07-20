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
package net.bluemind.mailbox.hook;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox;

public interface IMailboxHook {

	/**
	 * Invoked before the mailbox is created in the backing store
	 * 
	 * @param context
	 * @param domainUid
	 * @param name
	 * @throws ServerFault
	 */
	default void preMailboxCreated(BmContext context, String domainUid, ItemValue<Mailbox> value) {

	}

	public void onMailboxCreated(BmContext context, String domainUid, ItemValue<Mailbox> value) throws ServerFault;

	default void preMailboxUpdate(BmContext context, String domainUid, ItemValue<Mailbox> previousValue,
			ItemValue<Mailbox> value) {

	}

	public void onMailboxUpdated(BmContext context, String domainUid, ItemValue<Mailbox> previousValue,
			ItemValue<Mailbox> value) throws ServerFault;

	public void onMailboxDeleted(BmContext context, String domainUid, ItemValue<Mailbox> value) throws ServerFault;

	public void onMailFilterChanged(BmContext context, String domainUid, ItemValue<Mailbox> mailbox, MailFilter filter)
			throws ServerFault;

	public void onDomainMailFilterChanged(BmContext context, String domainUid, MailFilter filter) throws ServerFault;

	default void preMailboxDeleted(BmContext context, String domainUid, ItemValue<Mailbox> value) throws ServerFault {

	}
}
