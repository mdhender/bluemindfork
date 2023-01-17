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

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.cmd.AppendCommand;
import net.bluemind.imap.endpoint.driver.AppendStatus;
import net.bluemind.lib.vertx.Result;

/**
 * <code>A003 OK [APPENDUID 38505 3955] APPEND completed</code>
 *
 */
public class AppendProcessor extends AuthenticatedCommandProcessor<AppendCommand> {

	@Override
	public Class<AppendCommand> handledType() {
		return AppendCommand.class;
	}

	@Override
	protected void checkedOperation(AppendCommand command, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		AppendStatus appendStatus = ctx.mailbox().append(command.folder(), command.flags(), command.deliveryDate(),
				command.buffer());
		ctx.write(command.raw().tag() + appendStatus.statusName() + "\r\n");
		completed.handle(Result.success());
	}

}
