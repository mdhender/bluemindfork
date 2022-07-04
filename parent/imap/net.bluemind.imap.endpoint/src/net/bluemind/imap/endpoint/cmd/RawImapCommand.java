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
import java.util.List;

import com.google.common.base.MoreObjects;

import io.netty.buffer.ByteBuf;
import net.bluemind.imap.endpoint.parsing.Part;

public class RawImapCommand {

	private final List<Part> parts;
	private String cmdRoot;
	private String tag;

	public RawImapCommand(List<Part> copy) {
		this.parts = copy;
		ByteBuf rootBuf = copy.get(0).buffer().duplicate();
		int tagIdx = rootBuf.indexOf(rootBuf.readerIndex(), rootBuf.readableBytes(), (byte) ' ');
		if (tagIdx > 0) {
			this.tag = rootBuf.toString(rootBuf.readerIndex(), tagIdx, StandardCharsets.US_ASCII);
			this.cmdRoot = rootBuf.toString(tagIdx + 1, rootBuf.readableBytes() - tagIdx - 1, StandardCharsets.US_ASCII)
					.toLowerCase();
		} else {
			this.tag = "";
			this.cmdRoot = rootBuf.toString(StandardCharsets.US_ASCII);
		}
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(RawImapCommand.class).add("tag", tag).add("cmd", cmdRoot)
				.add("parts", parts.size()).toString();
	}

	public String cmd() {
		return cmdRoot;
	}

	public List<Part> parts() {
		return parts;
	}

	public String tag() {
		return tag;
	}

}
