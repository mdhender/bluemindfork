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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.imap.endpoint.exec;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.cmd.ExpungeCommand;
import net.bluemind.imap.endpoint.driver.MailboxConnection;
import net.bluemind.imap.endpoint.driver.SelectedFolder;
import net.bluemind.imap.endpoint.driver.UpdateMode;
import net.bluemind.lib.vertx.Result;

public class ExpungeProcessor extends SelectedStateCommandProcessor<ExpungeCommand> {

	private static final Logger logger = LoggerFactory.getLogger(ExpungeProcessor.class);

	@Override
	public Class<ExpungeCommand> handledType() {
		return ExpungeCommand.class;
	}

	@Override
	protected void checkedOperation(ExpungeCommand command, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		MailboxConnection mailbox = ctx.mailbox();
		SelectedFolder folder = ctx.selected();
		List<Long> uids = mailbox.uids(folder, "+deleted");
		mailbox.updateFlags(folder, uids, UpdateMode.Add, Collections.singletonList("\\Expunged"));
		uids.stream().sorted(Collections.reverseOrder()).forEach(uid -> ctx.write("* " + uid + " EXPUNGE\r\n"));
		ctx.write(command.raw().tag() + " OK Completed\r\n");
		completed.handle(Result.success());
		logger.debug("{} expunged.", folder);
	}

}
