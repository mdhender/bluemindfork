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

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.streams.WriteStream;
import net.bluemind.lib.vertx.Result;
import net.bluemind.pop3.endpoint.MailboxConnection.ListItem;

public class ListItemStream implements WriteStream<ListItem> {

	private PopContext ctx;

	public ListItemStream(PopContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public WriteStream<ListItem> exceptionHandler(Handler<Throwable> handler) {
		return this;
	}

	@Override
	public Future<Void> write(ListItem data) {
		return ctx.writeFuture(wireFormat(data));
	}

	private String wireFormat(ListItem data) {
		return data.mailNumber + " " + data.size + "\r\n";
	}

	@Override
	public void write(ListItem data, Handler<AsyncResult<Void>> handler) {
		write(data).compose(v -> {
			handler.handle(Result.success());
			return null;
		}, ex -> {
			handler.handle(Result.fail(ex));
			return null;
		});
	}

	@Override
	public void end(Handler<AsyncResult<Void>> handler) {
		handler.handle(Result.success());
	}

	@Override
	public WriteStream<ListItem> setWriteQueueMaxSize(int maxSize) {
		return this;
	}

	@Override
	public boolean writeQueueFull() {
		return ctx.socket().writeQueueFull();
	}

	@Override
	public WriteStream<ListItem> drainHandler(Handler<Void> handler) {
		ctx.socket().drainHandler(handler::handle);
		return this;
	}

}
