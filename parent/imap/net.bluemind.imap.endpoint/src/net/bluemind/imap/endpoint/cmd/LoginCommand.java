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

	protected LoginCommand(RawImapCommand raw) {
		super(raw);
		FlatCommand flat = flattenAtoms(false);
		try {
			CommandReader cr = new CommandReader(flat);
			cr.command("login");
			this.login = cr.nextString();
			cr.nextSpace();
			this.password = cr.nextString();
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

}
