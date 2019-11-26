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
package net.bluemind.mailbox.identity.service;

import java.sql.SQLException;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.identity.api.IMailboxIdentity;
import net.bluemind.mailbox.identity.service.internal.MailboxIdentityService;
import net.bluemind.mailbox.persistence.MailboxStore;

public class MailboxIdentityServiceFactory
		implements ServerSideServiceProvider.IServerSideServiceFactory<IMailboxIdentity> {

	@Override
	public Class<IMailboxIdentity> factoryClass() {
		return IMailboxIdentity.class;
	}

	@Override
	public IMailboxIdentity instance(BmContext context, String... params) throws ServerFault {

		if (params == null || params.length < 2) {
			throw new ServerFault("wrong number of instance parameters");
		}
		return getService(context, params[0], params[1]);
	}

	public IMailboxIdentity getService(BmContext context, String domainUid, String mboxUid) throws ServerFault {

		ContainerStore containerStore = new ContainerStore(null, context.getDataSource(), context.getSecurityContext());

		ItemValue<Domain> domainValue = context.su().provider().instance(IDomains.class).get(domainUid);

		if (domainValue == null) {
			throw new ServerFault("domain " + domainUid + " not found", ErrorCode.NOT_FOUND);
		}
		String mboxesContainerUid = domainUid;
		Container mboxesContainer = null;
		try {
			mboxesContainer = containerStore.get(mboxesContainerUid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		if (mboxesContainer == null) {
			throw new ServerFault("container " + mboxesContainerUid + " not found");
		}

		Container boxContainer = null;
		try {
			boxContainer = containerStore.get(IMailboxAclUids.uidForMailbox(mboxUid));
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		if (boxContainer == null) {
			throw new ServerFault("mbox container " + mboxUid + " not found");
		}

		ItemStore itemStore = new ItemStore(context.getDataSource(), mboxesContainer, context.getSecurityContext());
		MailboxStore mboxStore = new MailboxStore(context.getDataSource(), mboxesContainer);

		Item mboxItem = null;
		Mailbox mboxValue = null;
		try {
			mboxItem = itemStore.get(mboxUid);
			if (mboxItem != null) {
				mboxValue = mboxStore.get(mboxItem);
			}
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		if (mboxItem == null) {
			throw new ServerFault("mbox " + mboxUid + " not found", ErrorCode.NOT_FOUND);
		}

		if (mboxValue == null) {
			throw new ServerFault("mbox " + mboxUid + " not found", ErrorCode.NOT_FOUND);
		}
		return new MailboxIdentityService(context, mboxesContainer, boxContainer, mboxItem, mboxValue,
				domainValue.value);
	}
}
