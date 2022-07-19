/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.backend.mail.replica.service.internal.hooks;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.replica.persistence.DeletedMailboxesStore;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.hook.IMailboxHook;

public class MailboxSubtreeHook implements IMailboxHook {

	private static final Logger logger = LoggerFactory.getLogger(MailboxSubtreeHook.class);

	@Override
	public void preMailboxMoved(BmContext context, String domainUid, ItemValue<Mailbox> boxItem) throws ServerFault {
		forgetDeletion(context, domainUid, boxItem);
	}

	@Override
	public void postMailboxMoved(BmContext context, String domainUid, ItemValue<Mailbox> boxItem) throws ServerFault {
		forgetDeletion(context, domainUid, boxItem);
	}

	@Override
	public void preMailboxCreated(BmContext context, String domainUid, ItemValue<Mailbox> boxItem) throws ServerFault {
		forgetDeletion(context, domainUid, boxItem);

		// subtree create moved to MailApiBoxStorage
	}

	@Override
	public void preMailboxUpdate(BmContext context, String domainUid, ItemValue<Mailbox> previousValue,
			ItemValue<Mailbox> value) {
		if (!previousValue.value.name.equals(value.value.name)) {
			// the new name might match a previously deleted mailbox
			forgetDeletion(context, domainUid, value);
		}
	}

	@Override
	public void onMailboxCreated(BmContext context, String domainUid, ItemValue<Mailbox> boxItem) throws ServerFault {
		// we used to initialize here but creating the subtree at preCreate time avoids
		// a race with Cyrus replication
	}

	private void forgetDeletion(BmContext context, String domainUid, ItemValue<Mailbox> mbox) {
		String name = mbox.value.name;
		DeletedDataMementos.forgetDeletion(context, domainUid, mbox.value);
		try {
			logger.info("Ensure we don't consider {}@{} as deleted", name, domainUid);
			DeletedMailboxesStore deletedData = new DeletedMailboxesStore(context.getDataSource());
			deletedData.deleteByName(domainUid, name);
		} catch (SQLException e) {
			logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void onMailboxUpdated(BmContext context, String domainUid, ItemValue<Mailbox> previousBoxItem,
			ItemValue<Mailbox> currentBoxItem) throws ServerFault {

	}

	@Override
	public void onMailboxDeleted(BmContext context, String domainUid, ItemValue<Mailbox> boxItem) throws ServerFault {

	}

	@Override
	public void onMailFilterChanged(BmContext context, String domainUid, ItemValue<Mailbox> mailbox, MailFilter filter)
			throws ServerFault {
	}

	@Override
	public void onDomainMailFilterChanged(BmContext context, String domainUid, MailFilter filter) throws ServerFault {
	}

	@Override
	public void preMailboxDeleted(BmContext context, String domainUid, ItemValue<Mailbox> value) throws ServerFault {
		DeletedDataMementos.preDelete(context, domainUid, value);
	}

}
