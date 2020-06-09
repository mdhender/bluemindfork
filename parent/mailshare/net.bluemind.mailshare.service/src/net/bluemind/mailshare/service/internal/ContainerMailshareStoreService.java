/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.mailshare.service.internal;

import java.sql.SQLException;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.service.DirEntryAndValue;
import net.bluemind.directory.service.DirEntryHandler;
import net.bluemind.directory.service.DirValueStoreService;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.identity.persistence.MailboxIdentityStore;
import net.bluemind.mailshare.api.Mailshare;

public class ContainerMailshareStoreService extends DirValueStoreService<Mailshare> {

	private MailboxIdentityStore identityStore;

	public ContainerMailshareStoreService(BmContext context, Container container, ItemValue<Domain> domain) {
		super(context, context.getDataSource(), context.getSecurityContext(), domain, container,
				DirEntry.Kind.MAILSHARE, null, new MailshareDirEntryAdapter(), new MailshareVCardAdapter(),
				new MailshareMailboxAdapter());

		identityStore = new MailboxIdentityStore(context.getDataSource());

	}

	@Override
	protected byte[] getDefaultImage() {
		return DirEntryHandler.EMPTY_PNG;
	}

	@Override
	protected void decorate(Item item, ItemValue<DirEntryAndValue<Mailshare>> value) throws ServerFault {
		super.decorate(item, value);
		value.value.value = Mailshare.fromMailbox(value.value.mailbox);
		value.value.value.card = value.value.vcard;
		value.value.value.orgUnitUid = value.value.entry.orgUnitUid;
	}

	@Override
	protected void deleteValue(Item item) throws ServerFault, SQLException {
		identityStore.delete(item);
		super.deleteValue(item);
	}

}
