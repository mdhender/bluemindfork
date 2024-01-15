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
package net.bluemind.mailbox.service.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.MailboxQuota;
import net.bluemind.mailbox.service.IMailboxesStorage;
import net.bluemind.server.api.Server;

public class VoidMailboxesStorage implements IMailboxesStorage {
	public static final IMailboxesStorage INSTANCE = new VoidMailboxesStorage();
	private Logger logger = LoggerFactory.getLogger(VoidMailboxesStorage.class);

	@Override
	public void delete(BmContext context, String domainUid, ItemValue<Mailbox> value) throws ServerFault {
		logger.warn("VOID MAILSTORAGE delete {}:{}", domainUid, value.uid);
	}

	@Override
	public void update(BmContext context, String domainUid, ItemValue<Mailbox> previousValue, ItemValue<Mailbox> value)
			throws ServerFault {
		logger.warn("VOID MAILSTORAGE update {}:{}", domainUid, value.uid);
	}

	@Override
	public void create(BmContext context, String domainUid, ItemValue<Mailbox> value) throws ServerFault {
		logger.warn("VOID MAILSTORAGE create {}:{}", domainUid, value.uid);
	}

	@Override
	public void initialize(BmContext context, ItemValue<Server> server) throws ServerFault {
		logger.warn("VOID MAILSTORAGE initialize {}", server.value.address());
	}

	@Override
	public boolean mailboxExist(BmContext context, String domainUid, ItemValue<Mailbox> mailbox) throws ServerFault {
		return false;
	}

	@Override
	public MailboxQuota getQuota(BmContext context, String domainUid, ItemValue<Mailbox> value) throws ServerFault {
		return null;
	}

	@Override
	public void move(String domainUid, ItemValue<Mailbox> mailbox, ItemValue<Server> sourceServer,
			ItemValue<Server> dstServer) {
		logger.warn("VOID MAILSTORAGE move");
	}

}
