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
import java.util.Optional;
import java.util.stream.Collectors;

import net.bluemind.imap.impl.IMAPResponse;

public class SetMailboxAnnotationCommand extends SimpleCommand<Boolean> {

	public SetMailboxAnnotationCommand(String mbox, String annoId, Map<String, String> keyValues) {
		super("SETANNOTATION " + toUtf7(mbox) + " \"" + annoId + "\" " + flatten(keyValues));
	}

	private static String flatten(Map<String, String> keyValues) {
		return keyValues.entrySet().stream().map(e -> {
			String k = e.getKey();
			if (!k.endsWith(".priv") && !k.endsWith(".shared")) {
				k = k + ".priv";
			}
			String v = Optional.ofNullable(e.getValue()).map(val -> String.format("\"%s\"", val)).orElse("NIL");
			return String.format("\"%s\" %s", k, v);
		}).collect(Collectors.joining(" ", "(", ")"));
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
