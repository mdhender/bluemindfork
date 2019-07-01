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
 * Tells the server with support an extension, eg. QRESYNC
 */
public class EnableCommand extends SimpleCommand<Boolean> {

	public EnableCommand(String capability, String... otherCapabilities) {
		super("ENABLE " + concatenated(capability, otherCapabilities));
	}

	private static final String concatenated(String capability, String... otherCapas) {
		StringBuilder sb = new StringBuilder();
		sb.append(capability);
		for (int i = 0; i < otherCapas.length; i++) {
			sb.append(' ').append(otherCapas[i]);
		}
		return sb.toString();
	}

	@Override
	public void responseReceived(List<IMAPResponse> rs) {
		data = rs.get(rs.size() - 1).isOk();
	}

}
