/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.imap.vertx;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import io.vertx.core.streams.WriteStream;
import net.bluemind.imap.vertx.VXStoreClient.Decoder;
import net.bluemind.imap.vertx.cmd.AppendResponse;
import net.bluemind.imap.vertx.cmd.SelectResponse;
import net.bluemind.imap.vertx.stream.WriteToRead;

public interface IAsyncStoreClient {

	CompletableFuture<ImapResponseStatus<SelectResponse>> select(String mailbox);

	CompletableFuture<Void> fetch(long uid, String part, WriteStream<Buffer> target, Decoder dec);

	default ReadStream<Buffer> fetch(long uid, String part, Decoder dec) {
		WriteToRead<Buffer> ret = new WriteToRead<>();
		fetch(uid, part, ret, dec);
		return ret;
	}

	CompletableFuture<ImapResponseStatus<AppendResponse>> append(String mailbox, Date receivedDate,
			Collection<String> flags, int streamSize, ReadStream<Buffer> eml);

	CompletableFuture<Void> close();

	boolean isClosed();

}