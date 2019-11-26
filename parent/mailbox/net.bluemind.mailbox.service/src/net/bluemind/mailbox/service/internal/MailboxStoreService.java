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

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.persistence.ChangelogStore.LogEntry;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.MailboxContainerType;
import net.bluemind.mailbox.identity.persistence.MailboxIdentityStore;
import net.bluemind.mailbox.persistence.MailFilterStore;
import net.bluemind.mailbox.persistence.MailboxStore;

public class MailboxStoreService extends ContainerStoreService<Mailbox> {

	private final MailFilterStore mailFilterStore;
	private final String origin;
	private MailboxIdentityStore identityStore;
	private MailboxStore mailboxStore;

	public MailboxStoreService(DataSource pool, SecurityContext securityContext, Container container) {
		super(pool, securityContext, container, MailboxContainerType.TYPE, new MailboxStore(pool, container));
		this.mailFilterStore = new MailFilterStore(pool, container);
		this.origin = securityContext.getOrigin();
		this.identityStore = new MailboxIdentityStore(pool);
		this.mailboxStore = new MailboxStore(pool, container);
	}

	@Override
	protected void createValue(Item item, Mailbox value) throws ServerFault, SQLException {
		super.createValue(item, value);
		try {
			mailFilterStore.set(item, new MailFilter());
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	protected void deleteValue(Item item) throws ServerFault, SQLException {
		doOrFail(() -> {
			try {
				identityStore.delete(item);
				mailFilterStore.delete(item);
			} catch (SQLException e) {
				throw ServerFault.sqlFault(e);
			}
			super.deleteValue(item);
			return null;
		});
	}

	public MailFilter getFilter(String mailboxUid) throws ServerFault {
		try {
			Item item = getItemStore().get(mailboxUid);
			if (item == null) {
				return null;
			} else {
				return mailFilterStore.get(item);
			}
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

	}

	public void setFilter(String mailboxUid, MailFilter filter) throws ServerFault {
		doOrFail(() -> {
			Item item = getItemStore().getForUpdate(mailboxUid);
			if (item == null) {
				throw ServerFault.notFound("mailbox " + mailboxUid + " not found");
			} else {
				mailFilterStore.set(item, filter);
				item = itemStore.touch(item.uid);
				changelogStore.itemUpdated(LogEntry.create(item.version, item.uid, item.externalId,
						securityContext.getSubject(), origin, item.id, 0));
			}
			return null;
		});
	}

	public List<String> outOfOffice() throws ServerFault {
		return doOrFail(() -> {
			return mailFilterStore.findOutOfOffice(new Date());
		});
	}

	public List<String> inOfOffice() throws ServerFault {
		return doOrFail(() -> {
			return mailFilterStore.findInOfOffice(new Date());
		});
	}

	public void markOutOfOfficeFilterUpToDate(String uid, boolean activated) throws ServerFault {
		doOrFail(() -> {
			mailFilterStore.markOutOfOffice(getItemStore().getForUpdate(uid), activated);
			return null;
		});
	}

	public void deleteEmailByAlias(String alias) {
		try {
			((MailboxStore) super.itemValueStore).deleteEmailByAlias(alias);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public List<String> allUids() throws ServerFault {
		try {
			return this.mailboxStore.allUids();
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

}
