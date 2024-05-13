/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
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
package net.bluemind.imap.vt.cmd;

import io.vertx.core.buffer.Buffer;

public class LoginCommand extends ReturnOkCommand {

	private String login;
	private byte[] pass;

	public LoginCommand(CommandContext ctx, String login, byte[] pass) {
		super(ctx);
		this.login = login;
		this.pass = pass;
	}

	@Override
	protected void buildCommand(Buffer b) {
		b.appendString("LOGIN ").appendString(login).appendString(" {" + pass.length + "+}\r\n");
		b.appendBytes(pass);
	}

}
