/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
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
import net.bluemind.imap.endpoint.cmd.UnsubscribeCommand;
import net.bluemind.imap.endpoint.driver.MailboxConnection;
import net.bluemind.lib.vertx.Result;

public class UnsubscribeProcessor extends AuthenticatedCommandProcessor<UnsubscribeCommand> {

	private static final Logger logger = LoggerFactory.getLogger(UnsubscribeProcessor.class);

	@Override
	public Class<UnsubscribeCommand> handledType() {
		return UnsubscribeCommand.class;
	}

	@Override
	protected void checkedOperation(UnsubscribeCommand command, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		MailboxConnection con = ctx.mailbox();
		String f = command.folder();
		boolean success = con.unsubscribe(f);
		if (!success) {
			logger.warn("[{}] unsub of {} failed but we don't care", ctx, f);
		}
		ctx.write(command.raw().tag() + " OK unsubscribe completed\r\n");
		completed.handle(Result.success());
	}
}
