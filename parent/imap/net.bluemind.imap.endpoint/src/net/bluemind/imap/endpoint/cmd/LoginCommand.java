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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.bluemind.imap.endpoint.EndpointRuntimeException;

public class LoginCommand extends AnalyzedCommand {

	private static final Pattern split = Pattern.compile("login \"??([\\w@\\.-]+)\"?? \"??([^\"]+)",
			Pattern.CASE_INSENSITIVE);

	private final String login;
	private final String password;

	protected LoginCommand(RawImapCommand raw) {
		super(raw);
		FlatCommand flat = flattenAtoms(true);
		Matcher matcher = split.matcher(flat.fullCmd);

		if (matcher.find()) {
			this.login = matcher.group(1);
			this.password = matcher.group(2);
		} else {
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
