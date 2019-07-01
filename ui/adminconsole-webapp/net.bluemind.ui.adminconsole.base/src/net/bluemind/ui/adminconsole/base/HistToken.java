/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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

package net.bluemind.ui.adminconsole.base;

import java.util.HashMap;
import java.util.Map;

public class HistToken {

	public String screen;
	public Map<String, String> req;

	public static HistToken parse(String s) {
		HistToken ht = new HistToken();
		String dec = B64.fromB64(s);
		String[] parts = dec.split("&");
		ht.req = new HashMap<>();
		for (String part : parts) {
			int idx = part.indexOf('=');
			String k = part.substring(0, idx);
			String v = part.substring(idx + 1);
			if ("screen".equals(k)) {
				ht.screen = v;
			} else {
				ht.req.put(k, v);
			}
		}
		return ht;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("screen=").append(screen);
		for (String k : req.keySet()) {
			String val = req.get(k);
			sb.append('&').append(k).append('=').append(val);

		}
		String encoded = B64.toB64(sb.toString());
		return encoded;
	}
}
