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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.google.common.base.MoreObjects;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.WriteStream;
import net.bluemind.imap.vertx.parsing.ImapChunker.ImapChunk;

public class StreamSinkProcessor implements IProcessorDelegate {

	private final Optional<WriteStream<Buffer>> sink;
	private final CompletableFuture<Void> over;
	private boolean expectStream = false;
	private final long uid;
	private final String part;
	private final String selected;
	private final List<String> textHistory;
	private String conId;

	public StreamSinkProcessor(String conId, String selected, long uid, String part, WriteStream<Buffer> sink) {
		this.conId = conId;
		this.selected = selected;
		this.uid = uid;
		this.part = part;
		this.over = new CompletableFuture<>();
		this.sink = Optional.of(sink);
		this.textHistory = new ArrayList<>();
	}

	@Override
	public void processText(ImapChunk ic) {
		textHistory.add(ic.buffer().toString(StandardCharsets.US_ASCII));
		byte firstByte = ic.buffer().getByte(0);
		if (firstByte == 'V') {
			if (!expectStream) {
				sink.ifPresent(WriteStream::end);
			}
			over.complete(null);
		} else if (firstByte == '*' && ic.buffer().getByte(ic.buffer().length() - 1) == '}') {
			expectStream = true;
		}
	}

	public CompletableFuture<Void> future() {
		return over;
	}

	@Override
	public Optional<WriteStream<Buffer>> delegateStream() {
		return sink;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(StreamSinkProcessor.class).add("con", conId).add("sel", selected)
				.add("uid", uid).add("part", part).add("history", textHistory).toString();
	}

}
