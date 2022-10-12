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
package net.bluemind.imap.endpoint.cmd;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.netty.buffer.ByteBuf;
import net.bluemind.imap.endpoint.parsing.Part;
import net.bluemind.imap.endpoint.parsing.Part.Type;

public abstract class AnalyzedCommand {

	private RawImapCommand raw;

	public static class FlatCommand {
		String fullCmd;
		ByteBuf[] literals;
	}

	protected AnalyzedCommand(RawImapCommand raw) {
		this.raw = raw;
	}

	public final RawImapCommand raw() {
		return raw;
	}

	protected FlatCommand flattenAtoms(boolean expandSmallAttoms) {
		return flattenAtoms(expandSmallAttoms, 0);
	}

	protected FlatCommand flattenAtoms(boolean expandSmallAttoms, int keepAsLiteral) {
		AtomicInteger litIdx = new AtomicInteger();
		String justCmd = raw.parts().stream().filter(p -> p.type() == Type.COMMAND).map(p -> {
			String cmdStr = p.buffer().toString(StandardCharsets.US_ASCII);
			if (p.continued()) {
				int idx = cmdStr.lastIndexOf('{');
				cmdStr = cmdStr.substring(0, idx) + "{ATOM_" + litIdx.getAndIncrement() + "}";
			}
			return cmdStr;
		}).collect(Collectors.joining());
		justCmd = justCmd.substring(raw.tag().length() + 1);

		FlatCommand flat = new FlatCommand();
		flat.fullCmd = justCmd;
		flat.literals = raw.parts().stream().filter(p -> p.type() == Type.LITERAL_CHUNK).map(Part::buffer)
				.toArray(ByteBuf[]::new);

		if (expandSmallAttoms) {
			for (int i = 0; i < flat.literals.length - keepAsLiteral; i++) {
				ByteBuf b = flat.literals[i];
				if (b.readableBytes() < 256) {
					flat.fullCmd = flat.fullCmd.replace("{ATOM_" + i + "}", b.toString(StandardCharsets.UTF_8));
				}
			}
		}

		return flat;
	}

}
