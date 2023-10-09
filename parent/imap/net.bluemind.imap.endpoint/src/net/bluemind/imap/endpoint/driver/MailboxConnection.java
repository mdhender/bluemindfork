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

import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import io.netty.buffer.ByteBuf;
import io.vertx.core.streams.WriteStream;
import net.bluemind.core.container.model.ItemFlagFilter;

/**
 * 
 */
/**
 * 
 */
public interface MailboxConnection {

	String login();

	/*
	 * Folders operations
	 */
	SelectedFolder select(String fName);

	default SelectedFolder refreshed(SelectedFolder saved) {
		return saved;
	}

	String create(String fName);

	boolean delete(String fName);

	boolean subscribe(String fName);

	boolean unsubscribe(String fName);

	String rename(String fName, String newName);

	List<ListNode> list(String reference, String mailboxPattern);

	/*
	 * Email operations
	 */
	CompletableFuture<Void> fetch(SelectedFolder selected, ImapIdSet idset, List<MailPart> fetchSpec,
			WriteStream<FetchedItem> output);

	QuotaRoot quota(SelectedFolder selected);

	AppendStatus append(SelectedFolder target, List<String> flags, Date deliveryDate, ByteBuf buffer);

	/**
	 * @param sf
	 * @param idset
	 * @param mode
	 * @param flags
	 * @return a new content version for the selected folder
	 */
	long updateFlags(SelectedFolder sf, ImapIdSet idset, UpdateMode mode, List<String> flags);

	int maxLiteralSize();

	CopyResult copyTo(SelectedFolder source, String folder, ImapIdSet idset);

	List<Long> uids(SelectedFolder sel, String query);

	/**
	 * Filter over item flags the given set. If onlyCheckpointed is set, only the
	 * sequences visible to the folder will be eligible.
	 * 
	 * Returns a list of imap uids suitable for updating flags.
	 * 
	 * @param sel
	 * @param set
	 * @param filter
	 * @param onlyCheckpointed
	 * @return
	 */
	List<Long> uidSet(SelectedFolder sel, ImapIdSet set, ItemFlagFilter filter, boolean onlyCheckpointed);

	/*
	 * Imap system stuff
	 */
	void idleMonitor(SelectedFolder selected, Consumer<SelectedMessage[]> changesConsumer);

	void notIdle();

	void close();

	NamespaceInfos namespaces();

	String imapAcl(SelectedFolder selected);

	default String logId() {
		return login();
	}

}
