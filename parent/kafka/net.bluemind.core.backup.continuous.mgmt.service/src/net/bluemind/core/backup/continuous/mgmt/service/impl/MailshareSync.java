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
package net.bluemind.core.backup.continuous.mgmt.service.impl;

import java.util.List;

import com.google.common.collect.Lists;

import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.core.backup.continuous.mgmt.api.BackupSyncOptions;
import net.bluemind.core.container.api.IRestoreDirEntryWithMailboxSupport;
import net.bluemind.core.rest.BmContext;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailshare.api.Mailshare;

public class MailshareSync extends DirEntryWithMailboxSync<Mailshare> {

	private static final List<String> TYPE_ORDER = Lists.newArrayList(//
			IMailboxAclUids.TYPE, //
			IMailReplicaUids.REPLICATED_MBOXES, //
			IMailReplicaUids.MAILBOX_RECORDS //
	);

	public MailshareSync(BmContext ctx, BackupSyncOptions opts, DomainKafkaState domKafkaState,
			IRestoreDirEntryWithMailboxSupport<Mailshare> getApi, DomainApis domainApis) {
		super(ctx, opts, getApi, domainApis, domKafkaState);
	}

	@Override
	protected List<String> containerTypeOrder() {
		return TYPE_ORDER;
	}

}
