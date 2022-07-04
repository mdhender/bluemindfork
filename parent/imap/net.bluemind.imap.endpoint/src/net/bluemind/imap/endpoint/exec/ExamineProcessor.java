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
import net.bluemind.imap.endpoint.cmd.ExamineCommand;
import net.bluemind.imap.endpoint.driver.MailboxConnection;
import net.bluemind.imap.endpoint.driver.SelectedFolder;
import net.bluemind.lib.vertx.Result;

/**
 * 
 * <code>
 * * 7 EXISTS
 * 0 RECENT
 * FLAGS (\Answered \Flagged \Draft \Deleted \Seen)
 * OK [PERMANENTFLAGS (\Answered \Flagged \Draft \Deleted \Seen \*)] Ok
 * OK [UNSEEN 1] Ok
 * OK [UIDVALIDITY 1626984990] Ok
 * OK [UIDNEXT 28] Ok
 * OK [HIGHESTMODSEQ 3231] Ok
 * OK [URLMECH INTERNAL] Ok
 * OK [ANNOTATIONS 65536] Ok
 * . OK [READ-WRITE] Completed
 * </code>
 * 
 *
 */
public class ExamineProcessor extends AuthenticatedCommandProcessor<ExamineCommand> {

	private static final Logger logger = LoggerFactory.getLogger(ExamineProcessor.class);

	@Override
	public void checkedOperation(ExamineCommand sc, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		MailboxConnection con = ctx.mailbox();
		long time = System.currentTimeMillis();
		SelectedFolder selected = con.select(sc.folder());

		if (selected == null) {
			missingFolder(sc, ctx, completed);
			return;
		}

		StringBuilder resp = new StringBuilder();
		if (ctx.state() == SessionState.SELECTED) {
			resp.append("* OK [CLOSED] Ok\r\n");
		}
		resp.append("* " + selected.exist + " EXISTS\r\n");
		resp.append("* 0 RECENT\r\n");
		resp.append("* FLAGS (\\Answered \\Flagged \\Draft \\Deleted \\Seen)\r\n");
		resp.append("* OK [PERMANENTFLAGS (\\Answered \\Flagged \\Draft \\Deleted \\Seen \\*)] Ok\r\n");
		resp.append("* OK [UNSEEN " + selected.unseen + "] Ok\r\n");
		resp.append("* OK [UIDVALIDITY " + selected.folder.value.uidValidity + "] Ok\r\n");
		resp.append("* OK [UIDNEXT " + (selected.folder.value.lastUid + 1) + "] Ok\r\n");
		resp.append("* OK [HIGHESTMODSEQ " + selected.folder.value.highestModSeq + "] Ok\r\n");
		resp.append(sc.raw().tag() + " OK [READ-ONLY] Completed\r\n");

		ctx.write(resp.toString());
		time = System.currentTimeMillis() - time;
		if (logger.isInfoEnabled()) {
			logger.info("Examined in {}ms {} => {} ", time, sc.folder(), selected.folder);
		}

		completed.handle(Result.success());
	}

	private void missingFolder(ExamineCommand sc, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		if (ctx.state() == SessionState.SELECTED) {
			ctx.state(SessionState.AUTHENTICATED);
			ctx.selected(null);
		}
		ctx.write("* OK [CLOSED] Ok\r\n" + sc.raw().tag() + " NO Mailbox does not exist\r\n");
		completed.handle(Result.success());
	}

	@Override
	public Class<ExamineCommand> handledType() {
		return ExamineCommand.class;
	}

}
