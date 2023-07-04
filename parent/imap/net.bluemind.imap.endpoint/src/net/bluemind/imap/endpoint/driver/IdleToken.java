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
package net.bluemind.imap.endpoint.driver;

import java.util.Set;
import java.util.stream.Collectors;

import io.vertx.core.buffer.Buffer;

public interface IdleToken {

	Buffer toBuffer();

	public static class CountToken implements IdleToken {
		public final int count;
		public final String kind;

		public CountToken(String kind, int count) {
			this.kind = kind;
			this.count = count;
		}

		@Override
		public Buffer toBuffer() {
			Buffer buf = Buffer.buffer();
			buf.appendString("* " + count + " " + kind + "\r\n");
			return buf;
		}

	}

	public static record FetchToken(int seq, long uid, Set<String> flags) implements IdleToken {

		@Override
		public Buffer toBuffer() {
			Buffer buf = Buffer.buffer();
			buf.appendString("* " + seq + " FETCH (FLAGS (" + flags.stream().collect(Collectors.joining(" ")) + ") UID "
					+ uid + ")\r\n");
			return buf;
		}
	}

}
