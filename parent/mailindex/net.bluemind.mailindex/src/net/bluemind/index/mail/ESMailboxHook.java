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
package net.bluemind.index.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.replica.indexing.IMailIndexService;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.index.MailIndexActivator;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.hook.DefaultMailboxHook;

public class ESMailboxHook extends DefaultMailboxHook {

	private static final Logger logger = LoggerFactory.getLogger(ESMailboxHook.class);

	@Override
	public void onMailboxCreated(BmContext context, String domainUid, ItemValue<Mailbox> value) throws ServerFault {
		if (domainUid.equals("global.virt")) {
			return;
		}
		IMailIndexService indexService = MailIndexActivator.getService();
		if (indexService != null) {
			indexService.createMailbox(value.uid);
		} else {
			logger.warn("Index service missing onMailboxCreated (domain {})", domainUid);
		}
	}

	@Override
	public void onMailboxUpdated(BmContext context, String domainUid, ItemValue<Mailbox> previousValue,
			ItemValue<Mailbox> value) throws ServerFault {
	}

	@Override
	public void onMailboxDeleted(BmContext context, String domainUid, ItemValue<Mailbox> value) throws ServerFault {
		if (domainUid.equals("global.virt")) {
			return;
		}

		MailIndexActivator.getService().deleteMailbox(value.uid);

	}

}
