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

import java.util.Base64;
import java.util.List;

import net.bluemind.imap.impl.IMAPResponse;

public class SetMessageAnnotationCommand extends SimpleCommand<Boolean> {

	public SetMessageAnnotationCommand(int uid, String annoId, String value) {
		super("UID STORE " + uid + " ANNOTATION (" + annoId + " (value.priv \"" + b64(value) + "\"))");
	}

	private static String b64(String value) {
		return Base64.getUrlEncoder().encodeToString(value.getBytes());
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
