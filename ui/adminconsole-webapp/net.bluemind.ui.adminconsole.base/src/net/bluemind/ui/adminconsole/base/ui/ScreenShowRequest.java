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
package net.bluemind.ui.adminconsole.base.ui;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class ScreenShowRequest {

	private final Map<String, Object> req;

	public ScreenShowRequest() {
		req = new HashMap<>();
	}

	public void put(String k, Object v) {
		req.put(k, v);

	}

	public Object get(String k) {
		return req.get(k);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (String s : req.keySet()) {
			sb.append(" * ").append(s).append(" => ").append(req.get(s));
			sb.append('\n');
		}
		return sb.toString();
	}

	public Collection<String> keySet() {
		return req.keySet();
	}

}
