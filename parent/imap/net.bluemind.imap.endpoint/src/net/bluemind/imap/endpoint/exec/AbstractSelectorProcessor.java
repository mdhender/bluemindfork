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
import net.bluemind.imap.endpoint.SessionState;
import net.bluemind.imap.endpoint.cmd.AbstractFolderNameCommand;
import net.bluemind.imap.endpoint.driver.MailboxConnection;
import net.bluemind.imap.endpoint.driver.SelectedFolder;
import net.bluemind.lib.vertx.Result;

public abstract class AbstractSelectorProcessor<T extends AbstractFolderNameCommand>
		extends AuthenticatedCommandProcessor<T> {
	private static final Logger logger = LoggerFactory.getLogger(AbstractSelectorProcessor.class);

	protected boolean isAlwaysReadOnly() {
		return true;
	}

	@Override
	public void checkedOperation(T sc, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		MailboxConnection con = ctx.mailbox();
		long time = System.currentTimeMillis();
		SelectedFolder selected = con.select(sc.folder());

		if (selected == null) {
			missingFolder(sc, ctx, completed);
			return;
		}

		StringBuilder resp = new StringBuilder();
		resp.append("* " + selected.exist + " EXISTS\r\n");
		resp.append("* 0 RECENT\r\n");
		resp.append("* FLAGS (\\Answered \\Flagged \\Draft \\Deleted \\Seen)\r\n");
		resp.append("* OK [PERMANENTFLAGS (\\Answered \\Flagged \\Draft \\Deleted \\Seen \\*)] Ok\r\n");
		resp.append("* OK [UNSEEN " + selected.unseen + "] Ok\r\n");
		resp.append("* OK [UIDVALIDITY " + selected.folder.value.uidValidity + "] Ok\r\n");
		resp.append("* OK [UIDNEXT " + (selected.folder.value.lastUid + 1) + "] Ok\r\n");
		resp.append("* OK [HIGHESTMODSEQ " + selected.folder.value.highestModSeq + "] Ok\r\n");
		if (isAlwaysReadOnly() || selected.mailbox.readOnly) {
			resp.append(sc.raw().tag() + " OK [READ-ONLY] Completed\r\n");
		} else {
			resp.append(sc.raw().tag() + " OK [READ-WRITE] Completed\r\n");
		}

		ctx.state(SessionState.SELECTED);
		ctx.selected(selected);
		ctx.write(resp.toString());
		time = System.currentTimeMillis() - time;
		if (logger.isInfoEnabled()) {
			logger.info("Selected in {}ms {} => {} ", time, sc.folder(), selected.folder);
		}

		completed.handle(Result.success());
	}

	private void missingFolder(T sc, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		if (ctx.state() == SessionState.SELECTED) {
			ctx.state(SessionState.AUTHENTICATED);
			ctx.selected(null);
		}
		ctx.write("* OK [CLOSED] Ok\r\n" + sc.raw().tag() + " NO Mailbox does not exist\r\n");
		completed.handle(Result.success());
	}
}
