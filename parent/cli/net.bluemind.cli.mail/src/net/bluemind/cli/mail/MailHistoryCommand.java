/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.cli.mail;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.github.freva.asciitable.AsciiTable;

import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxRecordItemUri;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemChangeLogEntry;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;

public abstract class MailHistoryCommand implements ICmdLet, Runnable {

	protected CliContext ctx;
	private IContainers containerService;

	@Override
	public abstract void run();

	protected void printTable(List<ItemHistory> items) {
		Map<ItemChangeLogEntry, ItemHistory> changelog = new TreeMap<>(this::compareChangeLogEntry);
		items.forEach(item -> item.itemChangelog.entries.forEach(entry -> changelog.put(entry, item)));

		String[] headers = { "Date", "Type", "Owner", "Folder-UID", "Folder", "Item/IMAP-UID", "Flags" };
		String[][] data = new String[changelog.size()][headers.length];
		int index = 0;
		for (Entry<ItemChangeLogEntry, ItemHistory> entry : changelog.entrySet()) {
			ItemChangeLogEntry clogEntry = entry.getKey();
			ItemHistory item = entry.getValue();

			data[index][0] = clogEntry.date.toString();
			data[index][1] = clogEntry.type.name();
			data[index][2] = item.user;
			data[index][3] = item.containerDescriptor.uid;
			data[index][4] = item.containerDescriptor.name;
			data[index][5] = item.itemUid;
			data[index][6] = item.flags();
			index++;
		}

		cleanupFlags(data);

		ctx.info(AsciiTable.getTable(headers, data));
	}

	private void cleanupFlags(String[][] data) {
		Set<String> handledItemUids = new HashSet<>();
		for (int i = data.length - 1; i >= 0; i--) {
			String itemUid = data[i][5];
			if (handledItemUids.contains(itemUid)) {
				data[i][6] = "";
			} else {
				handledItemUids.add(itemUid);
			}
		}
	}

	protected ItemHistory getHistory(MailboxRecordItemUri ref) {
		ContainerDescriptor containerDescriptor = containerService.get(ref.containerUid);
		String user = getOwner(containerDescriptor);
		IDbMailboxRecords recordsApi = ctx.adminApi().instance(IDbMailboxRecords.class,
				IMailReplicaUids.uniqueId(ref.containerUid));
		Collection<String> flags = recordsApi.getComplete(ref.itemUid).flags.stream().map(f -> f.name())
				.collect(Collectors.toList());
		ItemChangelog itemChangelog = recordsApi.itemChangelog(ref.itemUid, 0l);
		return new ItemHistory(containerDescriptor, ref.itemUid, ref.bodyGuid, itemChangelog, user, flags);
	}

	private String getOwner(ContainerDescriptor containerDescriptor) {
		ListResult<ItemValue<DirEntry>> ownerSearch = ctx.adminApi()
				.instance(IDirectory.class, containerDescriptor.domainUid)
				.search(DirEntryQuery.filterEntryUid(containerDescriptor.owner));
		String user = null;
		if (ownerSearch.total > 0) {
			ItemValue<DirEntry> dirEntry = ownerSearch.values.get(0);
			user = String.format("%s (%s)", dirEntry.displayName, dirEntry.value.kind.name());
		} else {
			user = containerDescriptor.owner;
		}
		return user;
	}

	private int compareChangeLogEntry(ItemChangeLogEntry entry1, ItemChangeLogEntry entry2) {
		return Long.compare(entry1.date.getTime(), entry2.date.getTime());
	}

	public static class ItemHistory {
		public final ContainerDescriptor containerDescriptor;
		public final ItemChangelog itemChangelog;
		public final String user;
		public final String itemUid;
		public final Collection<String> flags;
		public final String bodyGuid;

		public ItemHistory(ContainerDescriptor containerDescriptor, String itemUid, String bodyGuid,
				ItemChangelog itemChangelog, String user, Collection<String> flags) {
			this.containerDescriptor = containerDescriptor;
			this.itemUid = itemUid;
			this.bodyGuid = bodyGuid;
			this.itemChangelog = itemChangelog;
			this.user = user;
			this.flags = flags;
		}

		public String flags() {
			return String.join(",", flags);
		}

	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.containerService = ctx.adminApi().instance(IContainers.class);
		return this;
	}

}
