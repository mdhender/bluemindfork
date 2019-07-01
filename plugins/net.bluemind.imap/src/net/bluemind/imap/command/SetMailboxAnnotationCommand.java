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
package net.bluemind.imap.command;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.bluemind.imap.impl.IMAPResponse;

public class SetMailboxAnnotationCommand extends SimpleCommand<Boolean> {

	public SetMailboxAnnotationCommand(String mbox, String annoId, Map<String, String> keyValues) {
		super("SETANNOTATION " + toUtf7(mbox) + " \"" + annoId + "\" (" + flatten(keyValues) + ")");
	}

	private static String flatten(Map<String, String> keyValues) {
		boolean first = true;
		StringBuilder sb = new StringBuilder();
		for (Entry<String, String> entry : keyValues.entrySet()) {
			if (!first) {
				sb.append(' ');
				first = false;
			}
			String k = entry.getKey();
			if (!k.endsWith(".priv") && !k.endsWith(".shared")) {
				k = k + ".priv";
			}
			sb.append('"').append(k).append('"');

			sb.append(' ');

			String v = entry.getValue();
			if (v == null) {
				sb.append("NIL");
			} else {
				sb.append('"').append(v).append('"');
			}
		}
		return sb.toString();
	}

	@Override
	public void responseReceived(List<IMAPResponse> rs) {
		IMAPResponse last = rs.get(rs.size() - 1);
		data = last.isOk();
		if (!data) {
			logger.warn("C: {} => S: {}", rawCommand(), last.getPayload());
		}
	}

}
