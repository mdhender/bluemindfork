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

/**
 * Creates a directory
 * 
 *
 */
public class CreateCommand extends SimpleCommand<Boolean> {

	public CreateCommand(String mailbox) {
		super("CREATE " + toUtf7(mailbox));
	}

	public CreateCommand(String mailbox, String specialUse) {
		super(String.format("CREATE %s (USE (%s))", toUtf7(mailbox), specialUse));
	}

	@Override
	public void responseReceived(List<IMAPResponse> rs) {
		data = rs.get(rs.size() - 1).isOk();
		if (Boolean.FALSE.equals(data)) {
			logger.error("C: {}", command);
			for (IMAPResponse ir : rs) {
				logger.error("S: {}", ir.getPayload());
			}
		}
	}

}
