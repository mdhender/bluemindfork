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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.imap.endpoint.driver;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import com.google.common.base.MoreObjects;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.container.model.ItemValue;

public class SelectedFolder {

	public final ItemValue<MailboxReplica> folder;
	public final long exist;
	public final long unseen;
	public final String partition;
	public final IDbMailboxRecords recApi;
	public final ImapMailbox mailbox;
	public final List<String> labels;
	public final long contentVersion;
	public final SelectedMessage[] sequences;
	public final AtomicLong notifiedContentVersion;

	public SelectedFolder(ImapMailbox mailbox, ItemValue<MailboxReplica> f, IDbMailboxRecords recApi, String partition,
			long exist, long unseen, List<String> labels, long contentVersion, SelectedMessage[] sequences) {
		this.mailbox = mailbox;
		this.folder = f;
		this.partition = partition;
		this.exist = exist;
		this.unseen = unseen;
		this.recApi = recApi;
		this.labels = labels;
		this.contentVersion = contentVersion;
		this.sequences = sequences;
		this.notifiedContentVersion = new AtomicLong(contentVersion);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(SelectedFolder.class).add("n", folder.value.fullName).add("v", contentVersion)
				.toString();
	}

	public Map<Long, Integer> imapUidToSeqNum() {
		Map<Long, Integer> uidToSeq = new Long2IntOpenHashMap((int) (sequences.length * 1.3));
		for (int i = 0; i < sequences.length; i++) {
			uidToSeq.put(sequences[i].imapUid(), i + 1);
		}
		return uidToSeq;
	}

	public Map<Long, Integer> internalIdToSeqNum() {
		Map<Long, Integer> uidToSeq = new Long2IntOpenHashMap((int) (sequences.length * 1.3));
		for (int i = 0; i < sequences.length; i++) {
			uidToSeq.put(sequences[i].internalId(), i + 1);
		}
		return uidToSeq;
	}

	public List<Long> imapUids() {
		LongArrayList col = new LongArrayList(sequences.length);
		Arrays.stream(sequences).mapToLong(r -> r.imapUid()).forEach(col::add);
		return col;
	}

	public List<Long> internalIds() {
		return internalIds(sm -> true);
	}

	public List<Long> internalIds(Predicate<SelectedMessage> sm) {
		LongArrayList col = new LongArrayList(sequences.length);
		Arrays.stream(sequences).filter(sm).mapToLong(r -> r.internalId()).forEach(col::add);
		return col;
	}

	public Set<Long> internalIdsSet() {
		Set<Long> col = new HashSet<>();
		Arrays.stream(sequences).mapToLong(r -> r.internalId()).forEach(col::add);
		return col;
	}

}
