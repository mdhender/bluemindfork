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

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;
import net.bluemind.imap.vt.parsing.IncomingChunk;
import net.bluemind.imap.vt.parsing.UTF7Converter;

public class AppendCommand extends TaggedCommand<Integer> {

	private ByteBuf eml;
	private String folderName;

	public AppendCommand(CommandContext ctx, String folderName, ByteBuf eml) {
		super(ctx);
		this.folderName = folderName;
		this.eml = eml;
	}

	@Override
	protected void buildCommand(Buffer b) {
		b.appendString("APPEND \"" + UTF7Converter.encode(folderName) + "\" {" + eml.readableBytes() + "+}\r\n");
		@SuppressWarnings("deprecation")
		var asVx = Buffer.buffer(eml);
		b.appendBuffer(asVx);
	}

	@Override
	protected Integer processChunks(List<IncomingChunk> chunks) {
		IncomingChunk last = chunks.getLast();
		if (!last.isOk()) {
			return -1;
		} else {
			// A003 OK [APPENDUID 38505 3955] APPEND completed
			String t = last.pieces().getFirst().txt();
			int lastBracket = t.lastIndexOf(']');
			if (lastBracket == -1) {
				return -1;
			}
			int spaceBeforeUid = t.lastIndexOf(' ', lastBracket);
			if (spaceBeforeUid == -1) {
				return -1;
			}
			return Integer.parseInt(t.substring(spaceBeforeUid + 1, lastBracket));
		}
	}

}
