/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.imap.endpoint.parsing;

import io.netty.buffer.ByteBuf;

public class LiteralSize {

	public static class LiteralLength {
		private int len;
		private boolean inline;

		public int total() {
			return len;
		}

		public boolean inline() {
			return inline;
		}
	}

	private LiteralSize() {

	}

	/**
	 * Parses the literal size at the end of a command
	 * 
	 * @param b contains a string ending with "a{123}"
	 * @return 123
	 */
	public static final LiteralLength of(ByteBuf b) {
		LiteralLength ret = new LiteralLength();
		int len = b.readableBytes();
		int total = 0;
		if (len > 0) {
			int multiply = 1;
			if (b.getByte(len - 1) == '}') {
				byte cur = b.getByte(len - 2);
				for (int i = len - 3; cur != '{' && cur != ' '; i--) {
					if (cur != '+') {
						// 0 has ascii value 48
						int value = cur - 48;
						total += value * multiply;
						multiply *= 10;
					} else {
						ret.inline = true;
					}
					cur = b.getByte(i);
				}
			}
		}
		ret.len = total;
		return ret;
	}

}