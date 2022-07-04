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
import net.bluemind.imap.endpoint.SessionState;
import net.bluemind.imap.endpoint.cmd.DoneCommand;
import net.bluemind.lib.vertx.Result;

public class DoneProcessor extends IdlingStateCommandProcessor<DoneCommand> {

	@Override
	public void checkedOperation(DoneCommand command, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		ctx.state(SessionState.SELECTED);
		ctx.write(ctx.idlingTag() + " OK Completed\r\n");
		ctx.mailbox().notIdle();
		completed.handle(Result.success());
	}

	@Override
	public Class<DoneCommand> handledType() {
		return DoneCommand.class;
	}

}
