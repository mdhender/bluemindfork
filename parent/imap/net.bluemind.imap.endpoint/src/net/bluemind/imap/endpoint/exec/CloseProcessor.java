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
import net.bluemind.imap.endpoint.cmd.CloseCommand;
import net.bluemind.lib.vertx.Result;

public class CloseProcessor extends AuthenticatedCommandProcessor<CloseCommand> {

	private static final Logger logger = LoggerFactory.getLogger(CloseProcessor.class);

	@Override
	public void checkedOperation(CloseCommand command, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		StringBuilder sb = new StringBuilder();
		if (ctx.state() == SessionState.SELECTED) {
			sb.append("* OK [CLOSED] Ok\r\n");
			logger.info("[{}] closed {}", ctx.mailbox(), ctx.selected());
			ctx.state(SessionState.AUTHENTICATED);
			ctx.selected(null);
		}
		sb.append(command.raw().tag()).append(" OK Completed\r\n");
		ctx.write(sb.toString());
		completed.handle(Result.success());
	}

	@Override
	public Class<CloseCommand> handledType() {
		return CloseCommand.class;
	}

}
