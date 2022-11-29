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
package net.bluemind.imap.driver.mailapi;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;

import net.bluemind.authentication.api.AuthUser;
import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.imap.endpoint.driver.ImapMailbox;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;

public class FolderResolver {

	private static final Logger logger = LoggerFactory.getLogger(FolderResolver.class);

	private IServiceProvider prov;
	private String mailSharePrefix;
	private String userSharePrefix;
	private ImapMailbox myImapBox;
	private IMailboxes mboxApi;

	private static final List<String> WRITE_ACL = Collections.singletonList(Verb.Write.name());
	private static final List<String> READ_ACL = Collections.singletonList(Verb.Read.name());

	public FolderResolver(IServiceProvider userProv, IServiceProvider suProv, AuthUser me,
			ItemValue<Mailbox> myMailbox) {
		this.prov = userProv;

		Config conf = DriverConfig.get();
		this.userSharePrefix = conf.getString(DriverConfig.USER_VIRTUAL_ROOT) + "/";
		this.mailSharePrefix = conf.getString(DriverConfig.SHARED_VIRTUAL_ROOT) + "/";
		this.myImapBox = new ImapMailbox();
		myImapBox.domainUid = me.domainUid;
		myImapBox.foldersApi = prov.instance(IDbByContainerReplicatedMailboxes.class,
				IMailReplicaUids.subtreeUid(me.domainUid, myMailbox));
		myImapBox.owner = myMailbox;
		myImapBox.readable = true;
		myImapBox.readOnly = false;

		mboxApi = suProv.instance(IMailboxes.class, myImapBox.domainUid);
	}

	/**
	 * @param imapName utf8 encoded imap folder name (eg.
	 *                 <code>Dossiers partagés/ms1668684683734</code>)
	 * @return
	 */
	ImapMailbox resolveBox(String imapName) {
		if (imapName.startsWith(userSharePrefix)) {
			return mboxNameAfterPrefix(userSharePrefix, imapName);
		} else if (imapName.startsWith(mailSharePrefix)) {
			return mboxNameAfterPrefix(mailSharePrefix, imapName);
		} else {
			return myImapBox.forReplicaName(imapName);
		}

	}

	private ImapMailbox mboxNameAfterPrefix(String prefix, String imapName) {
		String afterPrefix = imapName.substring(prefix.length());
		int trail = afterPrefix.indexOf('/');
		String mboxChunk = trail == -1 ? afterPrefix : afterPrefix.substring(0, trail);
		ItemValue<Mailbox> mbox = mboxApi.byName(mboxChunk);
		if (mbox == null) {
			return null;
		}

		IDbReplicatedMailboxes foldersApi = prov.instance(IDbByContainerReplicatedMailboxes.class,
				IMailReplicaUids.subtreeUid(myImapBox.domainUid, mbox));
		ImapMailbox imapMbox = new ImapMailbox();
		imapMbox.domainUid = myImapBox.domainUid;
		imapMbox.owner = mbox;
		imapMbox.foldersApi = foldersApi;

		IContainerManagement mgmtApi = prov.instance(IContainerManagement.class,
				IMailboxAclUids.uidForMailbox(mbox.uid));
		imapMbox.readable = mgmtApi.canAccess(READ_ACL);
		imapMbox.readOnly = !mgmtApi.canAccess(WRITE_ACL);

		if (trail == -1) {
			// root
			if (mbox.value.type.sharedNs) {
				imapMbox.replicaName = mbox.value.name;
			} else {
				imapMbox.replicaName = "INBOX";
			}
		} else {
			if (mbox.value.type.sharedNs) {
				imapMbox.replicaName = afterPrefix;
			} else {
				imapMbox.replicaName = afterPrefix.substring(trail + 1);
			}
		}
		logger.debug("{} resolvedAs {} - {}", imapName, mbox.value, imapMbox.replicaName);

		return imapMbox;
	}

}
