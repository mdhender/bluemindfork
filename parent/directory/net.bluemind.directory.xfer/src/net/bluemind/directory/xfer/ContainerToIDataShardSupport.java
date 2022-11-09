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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.directory.xfer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainersFlatHierarchy;
import net.bluemind.core.container.api.IDataShardSupport;
import net.bluemind.core.container.api.IFlatHierarchyUids;
import net.bluemind.core.container.api.IOwnerSubscriptionUids;
import net.bluemind.core.container.api.IOwnerSubscriptions;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.deferredaction.api.IDeferredAction;
import net.bluemind.deferredaction.api.IDeferredActionContainerUids;
import net.bluemind.exchange.mapi.api.IMapiFolder;
import net.bluemind.exchange.mapi.api.IMapiFolderAssociatedInformation;
import net.bluemind.exchange.mapi.api.MapiFAIContainer;
import net.bluemind.exchange.mapi.api.MapiFolderContainer;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.notes.api.INote;
import net.bluemind.notes.api.INoteUids;
import net.bluemind.tag.api.ITagUids;
import net.bluemind.tag.api.ITags;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.ITodoUids;
import net.bluemind.webappdata.api.IWebAppDataUids;

public class ContainerToIDataShardSupport {

	private static final Logger logger = LoggerFactory.getLogger(ContainerToIDataShardSupport.class);

	private ContainerToIDataShardSupport() {
	}

	public static IDataShardSupport getService(IServiceProvider sp, Container container, String domainUid,
			String entryUid, String serverUid, ItemValue<Mailbox> mailbox) {
		try {
			switch (container.type) {
			case IAddressBookUids.TYPE:
				return sp.instance(IAddressBook.class, container.uid);
			case ICalendarUids.TYPE:
				return sp.instance(ICalendar.class, container.uid);
			case IDeferredActionContainerUids.TYPE:
				return sp.instance(IDeferredAction.class, container.uid);
			case ITodoUids.TYPE:
				return sp.instance(ITodoList.class, container.uid);
			case INoteUids.TYPE:
				return sp.instance(INote.class, container.uid);
			case ITagUids.TYPE:
				return sp.instance(ITags.class, container.uid);
			case IFlatHierarchyUids.TYPE:
				return sp.instance(IContainersFlatHierarchy.class, domainUid, entryUid);
			case IOwnerSubscriptionUids.TYPE:
				return sp.instance(IOwnerSubscriptions.class, domainUid, entryUid);
			case IMailReplicaUids.MAILBOX_RECORDS:
				return sp.instance(IDbMailboxRecords.class, IMailReplicaUids.uniqueId(container.uid));
			case MapiFolderContainer.TYPE:
				return sp.instance(IMapiFolder.class, container.uid);
			case IWebAppDataUids.TYPE:
				return sp.instance(IMapiFolder.class, container.uid);
			case MapiFAIContainer.TYPE:
				return sp.instance(IMapiFolderAssociatedInformation.class,
						MapiFAIContainer.localReplica(container.uid));
			case IMailReplicaUids.REPLICATED_MBOXES:
				CyrusPartition part = CyrusPartition.forServerAndDomain(serverUid, domainUid);
				final String replicatedMailboxIdentifier = mailbox.value.type.nsPrefix
						+ mailbox.value.name.replace(".", "^");
				return sp.instance(IDbReplicatedMailboxes.class, part.name, replicatedMailboxIdentifier);
			default:
				return null;
			}
		} catch (ServerFault sf) {
			logger.error("ServerFault: {}", sf.getMessage(), sf);
			return null;
		}
	}
}
