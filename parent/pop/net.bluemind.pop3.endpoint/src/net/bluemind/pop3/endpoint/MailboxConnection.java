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
package net.bluemind.pop3.endpoint;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;

import io.vertx.core.streams.WriteStream;

public interface MailboxConnection {

	void close();

	CompletableFuture<Stat> stat();

	public static class ListItem {
		int mailNumber;
		int size;

		public ListItem(int num, int len) {
			this.mailNumber = num;
			this.size = len;
		}

	}

	public static class UidlItem {
		String msgBodyUid;
		int mailNumber;

		public UidlItem(String str, int mn) {
			this.msgBodyUid = str;
			this.mailNumber = mn;
		}
	}

	CompletableFuture<Void> list(Pop3Context ctx, WriteStream<ListItem> output);

	CompletableFuture<Void> uidl(Pop3Context ctx, WriteStream<UidlItem> ouput);

	public CompletableFuture<Void> uidlUnique(Pop3Context ctx, Integer id);

	CompletableFuture<Retr> retr(Pop3Context ctx, String params);

	CompletableFuture<Void> top(TopItemStream stream, String msgId, String numberOfBodyLines, Pop3Context ctx);

	CompletableFuture<ConcurrentMap<Integer, MailItemData>> mapPopIdtoMailId();

	public CompletableFuture<Void> listUnique(Pop3Context ctx, Integer id);

	CompletableFuture<Boolean> delete(Pop3Context ctx, List<Long> mailsToDelete);

}
