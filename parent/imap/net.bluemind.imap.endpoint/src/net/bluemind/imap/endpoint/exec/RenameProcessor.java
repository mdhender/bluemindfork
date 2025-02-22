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
import net.bluemind.imap.endpoint.cmd.RenameCommand;
import net.bluemind.imap.endpoint.driver.MailboxConnection;
import net.bluemind.imap.endpoint.driver.SelectedFolder;

public class RenameProcessor extends AuthenticatedCommandProcessor<RenameCommand> {
	@Override
	protected void checkedOperation(RenameCommand renameCommand, ImapContext ctx,
			Handler<AsyncResult<Void>> completed) {
		MailboxConnection con = ctx.mailbox();
		SelectedFolder srcFolder = con.select(renameCommand.srcFolder());
		SelectedFolder dstFolder = con.select(renameCommand.dstFolder());
		String error = "";

		if (srcFolder == null) {
			error = "source folder does not exists";
		} else if (dstFolder != null) {
			error = "destination folder already exists";
		}

		if (!error.isBlank()) {
			ctx.write(renameCommand.raw().tag() + " NO rename failure: " + error + "\r\n").onComplete(completed);
		} else {
			String newName = con.rename(renameCommand.srcFolder(), renameCommand.dstFolder());
			if (newName != null) {
				ctx.write(renameCommand.raw().tag() + " OK rename completed\r\n").onComplete(completed);
			} else {
				ctx.write(renameCommand.raw().tag() + " NO rename failed\r\n").onComplete(completed);
			}
		}
	}
	
	@Override
	public Class<RenameCommand> handledType() {
		return RenameCommand.class;
	}

}
