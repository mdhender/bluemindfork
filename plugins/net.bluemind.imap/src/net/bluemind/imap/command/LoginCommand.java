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

public class LoginCommand extends Command<Boolean> {

	private String login;
	private String password;

	public LoginCommand(String login, String password) {
		this.login = login;
		this.password = password;
	}

	@Override
	public void responseReceived(List<IMAPResponse> rs) {
		data = rs.get(0).isOk();
		if (!data) {
			logger.warn(rs.get(0).getPayload());
		}
	}

	@Override
	protected CommandArgument buildCommand() {
		byte[] bytes = password.getBytes();
		StringBuilder sb = new StringBuilder(48);
		sb.append("LOGIN \"").append(login).append("\" ");
		sb.append('{').append(bytes.length).append("+}");
		return new CommandArgument(sb.toString(), bytes);
	}
}
