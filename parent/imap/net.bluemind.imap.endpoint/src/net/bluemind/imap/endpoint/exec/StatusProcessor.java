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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.cmd.StatusCommand;
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
public class StatusProcessor extends AuthenticatedCommandProcessor<StatusCommand> {

	private static final Logger logger = LoggerFactory.getLogger(StatusProcessor.class);

	@Override
	public void checkedOperation(StatusCommand sc, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		MailboxConnection con = ctx.mailbox();
		SelectedFolder selected = con.select(sc.folder());

		if (selected == null) {
			missingFolder(sc, ctx, completed);
			return;
		}

		StringBuilder resp = new StringBuilder();
		resp.append("* STATUS " + sc.folder() + " (");
		List<String> props = folderProperties(sc, selected);
		resp.append(props.stream().collect(Collectors.joining(" ")));
		resp.append(")\r\n");
		resp.append(sc.raw().tag() + " OK Completed\r\n");

		ctx.write(resp.toString());
		completed.handle(Result.success());
	}

	private List<String> folderProperties(StatusCommand sc, SelectedFolder selected) {
		List<String> props = new ArrayList<>(sc.properties().size());
		for (String p : sc.properties()) {
			switch (p.toUpperCase()) {
			case "MESSAGES":
				props.add("MESSAGES " + selected.exist);
				break;
			case "RECENT":
				props.add("RECENT 0");
				break;
			case "UIDNEXT":
				props.add("UIDNEXT " + (selected.folder.value.lastUid + 1));
				break;
			case "UNSEEN":
				props.add("UNSEEN " + selected.unseen);
				break;
			case "UIDVALIDITY":
				// TODO
				props.add("UIDVALIDITY 0");
				break;
			default:
				logger.warn("Unsupported prop {}", p);
				break;
			}
		}
		return props;
	}

	private void missingFolder(StatusCommand sc, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		ctx.write(sc.raw().tag() + " NO Mailbox does not exist\r\n");
		completed.handle(Result.success());
	}

	@Override
	public Class<StatusCommand> handledType() {
		return StatusCommand.class;
	}

}
