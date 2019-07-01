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

import net.bluemind.imap.impl.IMAPResponse;

public class SelectCommand extends SimpleCommand<Boolean> {

	public SelectCommand(String mailbox) {
		super("SELECT " + toUtf7(mailbox));
	}

	@Override
	public void responseReceived(List<IMAPResponse> rs) {
		if (logger.isDebugEnabled()) {
			for (IMAPResponse r : rs) {
				logger.debug("selectResponse: " + r.getPayload());
			}
		}
		IMAPResponse ok = rs.get(rs.size() - 1);
		data = ok.isOk();
		if (!data) {
			for (IMAPResponse r : rs) {
				logger.error("S: " + r.getPayload());
			}
		}
	}

}
