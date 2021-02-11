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
package net.bluemind.imap.vertx.parsing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.streams.WriteStream;
import net.bluemind.imap.vertx.parsing.ImapChunker.ImapChunk;
import net.bluemind.imap.vertx.parsing.ImapChunker.Type;
import net.bluemind.lib.vertx.Result;

public class ImapChunkProcessor implements WriteStream<ImapChunk> {

	private static final Logger logger = LoggerFactory.getLogger(ImapChunkProcessor.class);
	private IProcessorDelegate del;

	public void setDelegate(IProcessorDelegate del) {
		this.del = del;
	}

	@Override
	public ImapChunkProcessor exceptionHandler(Handler<Throwable> handler) {
		return this;
	}

	@Override
	public ImapChunkProcessor write(ImapChunk data) {
		logger.debug("on {}", data);
		if (del == null) {
			return this;
		}

		if (data.type() == Type.Text) {
			del.processText(data);
		} else {
			del.delegateStream().ifPresent(ws -> {
				ws.write(data.buffer());
				if (data.isLastChunk()) {
					ws.end();
				}
			});
		}
		return this;
	}

	@Override
	public ImapChunkProcessor write(ImapChunk data, Handler<AsyncResult<Void>> handler) {
		write(data);
		handler.handle(Result.success());
		return this;
	}

	@Override
	public void end() {
		if (del != null) {
			del.delegateStream().ifPresent(WriteStream::end);
		}
	}

	@Override
	public void end(Handler<AsyncResult<Void>> handler) {
		end();
		handler.handle(Result.success());
	}

	@Override
	public ImapChunkProcessor setWriteQueueMaxSize(int maxSize) {
		return this;
	}

	@Override
	public boolean writeQueueFull() {
		return del != null && del.delegateStream().map(WriteStream::writeQueueFull).orElse(false);
	}

	@Override
	public ImapChunkProcessor drainHandler(Handler<Void> handler) {
		if (del != null && del.delegateStream().isPresent()) {
			del.delegateStream().ifPresent(ws -> ws.drainHandler(handler));
		} else {
			handler.handle(null);
		}
		return this;
	}

}
