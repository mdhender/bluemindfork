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
package net.bluemind.backend.mail.replica.service.internal;

import java.sql.SQLException;
import java.util.List;

import net.bluemind.backend.cyrus.partitions.CyrusBoxes;
import net.bluemind.backend.cyrus.partitions.CyrusBoxes.ReplicatedBox;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.replica.api.ICyrusReplicationAnnotations;
import net.bluemind.backend.mail.replica.api.MailboxAnnotation;
import net.bluemind.backend.mail.replica.persistence.AnnotationStore;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;

public class CyrusAnnotationsService implements ICyrusReplicationAnnotations {

	private final AnnotationStore annoStore;
	private final BmContext context;

	public CyrusAnnotationsService(BmContext context, AnnotationStore annoStore) {
		this.annoStore = annoStore;
		this.context = context;
	}

	@Override
	public void storeAnnotation(MailboxAnnotation ss) {
		new RBACManager(context).forContainer(IMailboxAclUids.uidForMailbox(mbox(ss.mailbox))).check(Verb.Write.name());

		try {
			annoStore.store(ss);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void deleteAnnotation(MailboxAnnotation ss) {
		new RBACManager(context).forContainer(IMailboxAclUids.uidForMailbox(mbox(ss.mailbox))).check(Verb.Write.name());

		try {
			annoStore.delete(ss);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	public List<MailboxAnnotation> annotations(String mbox) {
		new RBACManager(context).forContainer(IMailboxAclUids.uidForMailbox(mbox(mbox))).check(Verb.Read.name());

		try {
			return annoStore.byMailbox(mbox);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

	}

	private String mbox(String mailbox) {
		ReplicatedBox box = CyrusBoxes.forCyrusMailbox(mailbox);
		String domain = CyrusPartition.forName(box.partition).domainUid;
		ItemValue<Mailbox> mboxItem = context.su().provider().instance(IMailboxes.class, domain)
				.byName(box.local.replace('^', '.'));
		if (mboxItem == null) {
			throw ServerFault.notFound("mailbox uid for " + box + " (domain " + domain + ") not found");
		}
		return mboxItem.uid;
	}

}
