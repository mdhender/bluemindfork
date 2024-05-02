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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;

import io.vertx.core.buffer.Buffer;
import net.bluemind.imap.vt.parsing.IncomingChunk;
import net.bluemind.lib.jutf7.UTF7Converter;

public class UidCopyCommand extends TaggedCommand<Map<Integer, Integer>> {

	private static final Logger logger = LoggerFactory.getLogger(UidCopyCommand.class);
	private final String imapIdSet;
	private final String destFolder;

	public UidCopyCommand(CommandContext ctx, String destFolder, String imapIdSet) {
		super(ctx);
		this.destFolder = destFolder;
		this.imapIdSet = imapIdSet;
	}

	@Override
	protected void buildCommand(Buffer b) {
		b.appendString("UID COPY " + imapIdSet + " \"" + UTF7Converter.encode(destFolder) + "\"");
	}

	@Override
	protected Map<Integer, Integer> processChunks(List<IncomingChunk> chunks) {
		IncomingChunk last = chunks.getLast();
		if (!last.isOk()) {
			return Collections.emptyMap();
		} else {
			return processPayload(last.pieces().getFirst().txt());
		}
	}

	private Map<Integer, Integer> processPayload(String payload) {
		// A7 OK [COPYUID 1521549901 1:2,4 5:7] Completed

		String s = payload.substring(payload.indexOf("[") + 1, payload.indexOf("]"));
		Iterator<String> it = Splitter.on(" ").split(s).iterator();
		it.next(); // command
		it.next();// uid validity
		String source = it.next();
		String destination = it.next();

		logger.debug("source {}", source);
		logger.debug("destination {}", destination);

		Iterator<Integer> keys = asLongCollection(source).iterator();
		Iterator<Integer> values = asLongCollection(destination).iterator();

		Map<Integer, Integer> ret = new HashMap<>();
		while (keys.hasNext()) {
			ret.put(keys.next(), values.next());
		}

		return ret;
	}

	private static List<Integer> asLongCollection(String set) {
		String[] parts = set.split(",");
		ArrayList<Integer> ret = new ArrayList<>(64);
		for (String s : parts) {
			if (!s.contains(":")) {
				ret.add(Integer.parseInt(s));
			} else {
				String[] p = s.split(":");
				int start = Integer.parseInt(p[0]);
				int end = Integer.parseInt(p[1]);
				for (int l = start; l <= end; l++) {
					ret.add(l);
				}
			}
		}
		return ret;
	}

}
