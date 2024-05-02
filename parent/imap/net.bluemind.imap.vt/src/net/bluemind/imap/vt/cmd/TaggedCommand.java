/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
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
package net.bluemind.imap.vt.cmd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.vertx.core.buffer.Buffer;
import net.bluemind.imap.vt.parsing.IncomingChunk;

public abstract class TaggedCommand<T> {

	private static final byte[] CRLF = "\r\n".getBytes();
	private final CommandContext ctx;

	protected TaggedCommand(CommandContext ctx) {
		this.ctx = ctx;
	}

	public T execute() throws IOException {
		Buffer buf = Buffer.buffer();
		String tag = ctx.tagProd().nextTag() + " ";

		buf.appendBytes(tag.getBytes());
		buildCommand(buf);
		buf.appendBytes(CRLF);

		ctx.out().write(buf.getBytes());
		return processChunks(untilTag(tag));
	}

	private List<IncomingChunk> untilTag(String tag) {
		List<IncomingChunk> chunks = new ArrayList<>();
		try {
			do {
				IncomingChunk chunk = ctx.pending().poll(1, TimeUnit.SECONDS);
				if (chunk != null) {
					chunks.add(chunk);
					if (chunk.tagged(tag)) {
						break;
					}
				}
			} while (true);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		return chunks;
	}

	/**
	 * The given buffer already holds the command TAG and a space. CRLF will be
	 * added after <code>buildCommand</code> returns.
	 * 
	 * @param b
	 */
	protected abstract void buildCommand(Buffer b);

	protected abstract T processChunks(List<IncomingChunk> chunks) throws IOException;

}
