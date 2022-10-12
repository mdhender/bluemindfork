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

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.cmd.CreateCommand;
import net.bluemind.imap.endpoint.driver.MailboxConnection;
import net.bluemind.imap.endpoint.driver.SelectedFolder;
import net.bluemind.lib.vertx.Result;

public class CreateProcessor extends AuthenticatedCommandProcessor<CreateCommand> {
	@Override
	public Class<CreateCommand> handledType() {
		return CreateCommand.class;
	}

	@Override
	protected void checkedOperation(CreateCommand createCommand, ImapContext ctx,
			Handler<AsyncResult<Void>> completed) {
		MailboxConnection con = ctx.mailbox();
		SelectedFolder selected = con.select(createCommand.folder());
		if (selected != null) {
			ctx.write(createCommand.raw().tag() + " NO already exists\r\n");
		} else {
			con.create(createCommand.folder());
			ctx.write(createCommand.raw().tag() + " OK CREATE completed\r\n");
		}
		completed.handle(Result.success());
	}

}
