/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.imap.endpoint.cmd;

import net.bluemind.imap.endpoint.EndpointRuntimeException;

public class LoginCommand extends AnalyzedCommand {

	private final String login;
	private final String password;

	private record Credentials(String log, String pass) {

	}

	protected LoginCommand(RawImapCommand raw) {
		super(raw);
		FlatCommand flat = flattenAtoms(true);
		try {
			Credentials creds = parser(flat.fullCmd.toLowerCase());
			this.login = creds.log();
			this.password = creds.pass();
		} catch (Exception e) {
			throw new EndpointRuntimeException("Cannot split '" + flat.fullCmd + "'");
		}
	}

	public String login() {
		return login;
	}

	public String password() {
		return password;
	}

	private Credentials parser(String command) {
		String log = null;
		int lastLoginChar = 0;
		int loginStart = "login ".length();
		if (command.charAt(loginStart) == '"') {
			lastLoginChar = command.indexOf('"', loginStart + 2);
			log = command.substring(loginStart + 1, lastLoginChar);
			lastLoginChar += 2;
		} else {
			lastLoginChar = command.indexOf(' ', loginStart + 1);
			log = command.substring(loginStart, lastLoginChar);
			lastLoginChar += 1;
		}
		String pass = null;
		if (command.charAt(lastLoginChar) == '"') {
			int lastPasswordChar = command.lastIndexOf('"', command.length());
			pass = command.substring(lastLoginChar + 1, lastPasswordChar);
		} else {
			pass = command.substring(lastLoginChar, command.length());
		}
		return new Credentials(log, pass);
	}

}
