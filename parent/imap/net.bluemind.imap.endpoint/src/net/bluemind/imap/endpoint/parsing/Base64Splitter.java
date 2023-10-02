/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Base64Splitter {

	private Base64Splitter() {
	}

	public static List<String> splitOnNull(byte[] cur) {
		List<String> parts = new ArrayList<>(3);
		int start = 0;
		for (int i = 0; i < cur.length; i++) {
			if (cur[i] == 0x00) {
				parts.add(new String(cur, start, i - start, StandardCharsets.US_ASCII));
				start = i + 1;
			}
		}
		if (start < cur.length) {
			parts.add(new String(cur, start, cur.length - start, StandardCharsets.US_ASCII));
		}
		return parts;
	}

}
