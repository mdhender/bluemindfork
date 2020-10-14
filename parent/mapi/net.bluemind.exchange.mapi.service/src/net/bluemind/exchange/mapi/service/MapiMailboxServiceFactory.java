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
package net.bluemind.exchange.mapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.exchange.mapi.api.IMapiMailbox;
import net.bluemind.exchange.mapi.service.internal.MapiMailboxService;
import net.bluemind.exchange.publicfolders.common.PublicFolders;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;

public class MapiMailboxServiceFactory implements ServerSideServiceProvider.IServerSideServiceFactory<IMapiMailbox> {

	private static final Logger logger = LoggerFactory.getLogger(MapiMailboxServiceFactory.class);

	@Override
	public Class<IMapiMailbox> factoryClass() {
		return IMapiMailbox.class;
	}

	private IMapiMailbox getService(BmContext context, String domainUid, String mailboxUid) throws ServerFault {
		return new MapiMailboxService(context, domainUid, mailboxUid);
	}

	@Override
	public IMapiMailbox instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 2) {
			throw new ServerFault("wrong number of instance parameters");
		}
		String domain = params[0];
		String mboxUid = params[1];
		if (PublicFolders.mailboxGuid(domain).equals(mboxUid)) {
			logger.info("Public Folder hierarchy mbox for domain {}", domain);
		} else {
			IMailboxes mboxesApi = context.su().provider().instance(IMailboxes.class, domain);
			ItemValue<Mailbox> mailbox = mboxesApi.getComplete(mboxUid);

			if (mailbox == null) {
				throw new ServerFault("Mailbox " + mboxUid + " not found", ErrorCode.NOT_FOUND);
			}
		}
		return getService(context, domain, mboxUid);
	}

}
