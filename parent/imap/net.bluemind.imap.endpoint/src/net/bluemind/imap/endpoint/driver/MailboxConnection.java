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
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.netty.buffer.ByteBuf;
import io.vertx.core.streams.WriteStream;
import net.bluemind.core.container.model.ItemFlagFilter;

public interface MailboxConnection {

	String login();

	/*
	 * Folders operations
	 */
	SelectedFolder select(String fName);

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

	AppendStatus append(String folder, List<String> flags, Date deliveryDate, ByteBuf buffer);

	void updateFlags(SelectedFolder sf, ImapIdSet idset, UpdateMode mode, List<String> flags);

	int maxLiteralSize();

	CopyResult copyTo(SelectedFolder source, String folder, String idset);

	List<Long> uids(SelectedFolder sel, String query);

	Map<Long, Integer> sequences(SelectedFolder sel);

	List<Long> uidSet(SelectedFolder sel, String set, ItemFlagFilter filter);

	/*
	 * Imap system stuff
	 */
	void idleMonitor(SelectedFolder selected, WriteStream<IdleToken> ctx);

	void notIdle();

	void close();

	NamespaceInfos namespaces();

	String imapAcl(SelectedFolder selected);

	default String logId() {
		return login();
	}

}
