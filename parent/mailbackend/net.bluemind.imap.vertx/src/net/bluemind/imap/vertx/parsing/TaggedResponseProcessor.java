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

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import net.bluemind.imap.vertx.ImapResponseStatus;
import net.bluemind.imap.vertx.ImapResponseStatus.Status;
import net.bluemind.imap.vertx.parsing.ImapChunker.ImapChunk;

public class TaggedResponseProcessor<T> implements IProcessorDelegate {

	private final CompletableFuture<ImapResponseStatus<T>> future;
	private ResponsePayloadBuilder<T> builder;

	public TaggedResponseProcessor(ResponsePayloadBuilder<T> builder) {
		this.future = new CompletableFuture<>();
		this.builder = builder;
	}

	public CompletableFuture<ImapResponseStatus<T>> future() {
		return future;
	}

	@Override
	public void processText(ImapChunk ic) {
		byte firstByte = ic.buffer().getByte(0);
		if (firstByte == 'V') {
			String ascii = ic.buffer().toString(StandardCharsets.US_ASCII);
			int tagSep = ascii.indexOf(' ');
			String tag = ascii.substring(0, tagSep);
			int status = tagSep + 1;
			int next = ascii.indexOf(' ', status);
			String okNoBad = ascii.substring(status, next);
			String msg = ascii.substring(next + 1);
			if (builder.tagged(tag, Status.of(okNoBad), msg)) {
				future.complete(builder.build());
			}
		} else if (firstByte == '*') {
			doUntagged(ic);
		}
	}

	private void doUntagged(ImapChunk ic) {
		if (builder.untagged(ic.buffer())) {
			future.complete(builder.build());
		}
	}

	@Override
	public Optional<WriteStream<Buffer>> delegateStream() {
		return Optional.empty();
	}

}
