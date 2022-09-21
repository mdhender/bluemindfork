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
package net.bluemind.backend.mail.replica.service.internal.repair;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;

import net.bluemind.config.Token;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.imap.ListInfo;
import net.bluemind.imap.ListResult;
import net.bluemind.imap.StoreClient;
import net.bluemind.index.mail.Sudo;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.server.api.Server;

public abstract class MailboxWalk {
	protected final ItemValue<Mailbox> mbox;
	protected final String domainUid;
	protected final BmContext context;
	protected final Server srv;

	private MailboxWalk(BmContext context, ItemValue<Mailbox> mbox, String domainUid, Server srv) {
		this.srv = srv;
		this.context = context;
		this.mbox = mbox;
		this.domainUid = domainUid;
	}

	public static MailboxWalk create(BmContext context, ItemValue<Mailbox> mbox, String domainUid, Server srv) {
		if (mbox.value.type.sharedNs) {
			return new SharedMailboxWalk(context, mbox, domainUid, srv);
		} else {
			return new UserMailboxWalk(context, mbox, domainUid, srv);
		}
	}

	public abstract void folders(BiConsumer<StoreClient, List<ListInfo>> process, IServerTaskMonitor monitor);

	public static final class UserMailboxWalk extends MailboxWalk {

		public UserMailboxWalk(BmContext context, ItemValue<Mailbox> mbox, String domainUid, Server srv) {
			super(context, mbox, domainUid, srv);
		}

		public void folders(BiConsumer<StoreClient, List<ListInfo>> process, IServerTaskMonitor monitor) {
			String login = mbox.value.name + "@" + domainUid;

			try (Sudo sudo = new Sudo(mbox.value.name, domainUid);
					StoreClient sc = new StoreClient(srv.address(), 1143, login, sudo.context.getSessionId())) {
				if (!sc.login()) {
					monitor.log("[{}] Fail to connect", mbox.value.name);
					return;
				}
				ListResult allFolders = sc.listAll();
				process.accept(sc, allFolders);
			}
		}
	}

	public static final class SharedMailboxWalk extends MailboxWalk {

		public SharedMailboxWalk(BmContext context, ItemValue<Mailbox> mbox, String domainUid, Server srv) {
			super(context, mbox, domainUid, srv);
		}

		public void folders(BiConsumer<StoreClient, List<ListInfo>> process, IServerTaskMonitor monitor) {
			try (StoreClient sc = new StoreClient(srv.address(), 1143, "admin0", Token.admin0())) {
				if (!sc.login()) {
					monitor.log("Fail to connect as admin0 for {}", mbox.value.name);
					return;
				}
				List<ListInfo> mboxFoldersWithRoot = new LinkedList<>();
				ListInfo root = new ListInfo(mbox.value.name + "@" + domainUid, true);
				mboxFoldersWithRoot.add(root);
				ListResult shareChildren = sc.listSubFoldersMailbox(mbox.value.name + "@" + domainUid);
				mboxFoldersWithRoot.addAll(shareChildren);
				process.accept(sc, mboxFoldersWithRoot);
			}
		}
	}

}
