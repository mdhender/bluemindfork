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
package net.bluemind.imap.endpoint.parsing;

import com.google.common.base.MoreObjects;

import io.netty.buffer.ByteBuf;

public class Part {

	public enum Type {
		COMMAND, LITERAL_CHUNK
	}

	private final Type type;
	private final int expectedBytes;
	private final ByteBuf buffer;

	public Part(Type t, ByteBuf buf, int bytes) {
		this.type = t;
		this.buffer = buf;
		this.expectedBytes = bytes;
	}

	public static Part literalChunk(ByteBuf buf, int expectedBytes) {
		return new Part(Type.LITERAL_CHUNK, buf, expectedBytes);
	}

	public static Part endOfCommand(ByteBuf buf) {
		return new Part(Type.COMMAND, buf, 0);
	}

	public static Part followedByLiteral(ByteBuf buf) {
		return new Part(Type.COMMAND, buf, 1);
	}

	public boolean continued() {
		return expectedBytes > 0;
	}

	public int expected() {
		return expectedBytes;
	}

	public Type type() {
		return type;
	}

	public ByteBuf buffer() {
		return buffer;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(Part.class).add("t", type).add("continued", continued()).toString();
	}

	public void release() {
		buffer.release();
	}

}
