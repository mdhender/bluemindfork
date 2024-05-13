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

import io.vertx.core.buffer.Buffer;
import net.bluemind.imap.vt.dto.ListInfo;
import net.bluemind.imap.vt.dto.ListResult;
import net.bluemind.imap.vt.parsing.IncomingChunk;
import net.bluemind.imap.vt.parsing.UTF7Converter;

public class ListCommand extends TaggedCommand<ListResult> {

	private final String ref;
	private final String pattern;

	public ListCommand(CommandContext ctx, String ref, String pattern) {
		super(ctx);
		this.ref = ref;
		this.pattern = pattern;
	}

	public ListCommand(CommandContext ctx) {
		this(ctx, "", "*");
	}

	@Override
	protected void buildCommand(Buffer b) {
		b.appendString("XLIST \"" + ref + "\" \"" + pattern + "\"");
	}

	@Override
	protected ListResult processChunks(List<IncomingChunk> chunks) {
		if (chunks.getLast().isOk()) {
			ListResult ret = new ListResult();
			for (var ic : chunks) {
				String t = ic.pieces().getFirst().txt();
				if (t.startsWith("* XLIST (")) {
					ret.add(toListResult(t));
				}
			}
			return ret;
		} else {
			return new ListResult();
		}
	}

	private ListInfo toListResult(String p) {
		// * XLIST (\HasNoChildren \drafts) "/" "Drafts"
		int oParen = p.indexOf('(', 5);
		int cPren = p.indexOf(')', oParen);
		String flags = p.substring(oParen + 1, cPren);
		String mbox = UTF7Converter.decode(p.substring(cPren + 6, p.length()).replace("\"", ""));

		return new ListInfo(mbox, !flags.contains("\\NoSelect"));
	}

}
