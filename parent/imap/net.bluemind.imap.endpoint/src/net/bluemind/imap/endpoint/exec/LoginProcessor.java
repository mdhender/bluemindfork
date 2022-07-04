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
package net.bluemind.imap.endpoint.exec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.SessionState;
import net.bluemind.imap.endpoint.cmd.LoginCommand;
import net.bluemind.imap.endpoint.driver.Drivers;
import net.bluemind.imap.endpoint.driver.MailboxConnection;
import net.bluemind.imap.endpoint.driver.MailboxDriver;
import net.bluemind.lib.vertx.Result;

public class LoginProcessor implements CommandProcessor<LoginCommand> {

	private static final Logger logger = LoggerFactory.getLogger(LoginProcessor.class);

	private final Capabilities caps;

	public LoginProcessor() {
		this.caps = new Capabilities();
	}

	@Override
	public void operation(LoginCommand lc, ImapContext ctx, Handler<AsyncResult<Void>> completed) {

		MailboxDriver driver = Drivers.activeDriver();
		MailboxConnection connection = driver.open(lc.login(), lc.password());
		if (connection != null) {
			ctx.mailbox(connection);
			ctx.state(SessionState.AUTHENTICATED);
			ctx.write(lc.raw().tag() + " OK [CAPABILITY " + caps.all() + "] User logged in.\r\n");
			completed.handle(Result.success());
		} else {
			ctx.vertx().setTimer(3000, tid -> {
				logger.warn("Delay NO response to {} login attempt.", lc.login());
				ctx.write(lc.raw().tag() + " NO bad login or password.\r\n");
				completed.handle(Result.success());
			});
		}

	}

	@Override
	public Class<LoginCommand> handledType() {
		return LoginCommand.class;
	}

}
