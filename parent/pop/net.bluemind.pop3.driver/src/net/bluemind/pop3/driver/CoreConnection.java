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
package net.bluemind.pop3.driver;

import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Suppliers;
import com.google.common.collect.Iterables;

import io.vertx.core.streams.WriteStream;
import net.bluemind.authentication.api.AuthUser;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.Weight;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.pop3.endpoint.MailboxConnection;
import net.bluemind.pop3.endpoint.Stat;

public class CoreConnection implements MailboxConnection {

	private static final Logger logger = LoggerFactory.getLogger(CoreConnection.class);

	private IServiceProvider prov;
	private AuthUser me;
	private Supplier<ItemValue<MailboxFolder>> inboxRef = Suppliers.memoize(() -> inbox());

	public CoreConnection(IServiceProvider prov, AuthUser authUser) {
		this.prov = prov;
		this.me = authUser;
	}

	@Override
	public void close() {
		prov.instance(IAuthentication.class).logout();
		logger.info("{} disconnected", me.value.defaultEmailAddress());
	}

	@Override
	public Stat stat() {
		ItemValue<MailboxFolder> inbox = inboxRef.get();
		IDbMailboxRecords recApi = prov.instance(IDbMailboxRecords.class, inbox.uid);

		Count count = recApi.count(ItemFlagFilter.create().mustNot(ItemFlag.Deleted));
		Weight w = recApi.weight();
		return new Stat((int) count.total, w.total);
	}

	private ItemValue<MailboxFolder> inbox() {
		IMailboxFolders foldersApi = prov.instance(IMailboxFolders.class, me.domainUid, "user." + me.value.login);
		return foldersApi.byName("INBOX");
	}

	@Override
	public CompletableFuture<Void> list(WriteStream<ListItem> output) {
		ItemValue<MailboxFolder> inbox = inboxRef.get();
		IMailboxItems recApi = prov.instance(IMailboxItems.class, inbox.uid);
		ContainerChangeset<ItemVersion> cs = recApi.filteredChangesetById(0L,
				ItemFlagFilter.create().mustNot(ItemFlag.Deleted));
		Iterator<ItemVersion> recordIds = Iterables.concat(cs.created, cs.updated).iterator();

		CompletableFuture<Void> ret = new CompletableFuture<>();
		fetch(ret, recApi, recordIds, 1, output);
		return ret;
	}

	private void fetch(CompletableFuture<Void> ret, IMailboxItems recApi, Iterator<ItemVersion> recordIds, int seq,
			WriteStream<ListItem> output) {
		while (recordIds.hasNext()) {
			ItemVersion next = recordIds.next();
			ItemValue<MailboxItem> mail = recApi.getCompleteById(next.id);
			int len = mail.value.body.size;
			ListItem li = new ListItem(seq++, len);
			output.write(li);
			if (output.writeQueueFull()) {
				final int curSeq = seq;
				output.drainHandler(v -> fetch(ret, recApi, recordIds, curSeq, output));
				return;
			}
		}
		output.end();
		ret.complete(null);
	}

}
