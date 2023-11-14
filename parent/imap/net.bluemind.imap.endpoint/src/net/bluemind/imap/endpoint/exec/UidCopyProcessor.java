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
import net.bluemind.imap.endpoint.cmd.AnalyzedCommand;
import net.bluemind.imap.endpoint.cmd.UidCopyCommand;
import net.bluemind.imap.endpoint.driver.CopyResult;
import net.bluemind.imap.endpoint.driver.MailboxConnection;
import net.bluemind.imap.endpoint.driver.SelectedFolder;
import net.bluemind.imap.endpoint.locks.ISequenceWriter;

public class UidCopyProcessor extends SelectedStateCommandProcessor<UidCopyCommand> implements ISequenceWriter {

	private static final Logger logger = LoggerFactory.getLogger(UidCopyProcessor.class);

	@Override
	public Class<UidCopyCommand> handledType() {
		return UidCopyCommand.class;
	}

	@Override
	protected void checkedOperation(UidCopyCommand command, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		MailboxConnection con = ctx.mailbox();

		try {
			String targetFolder = command.folder();
			logger.debug("[{}] prepare copy to {}", con, targetFolder);
			CopyResult allocatedIds = con.copyTo(ctx.selected(), targetFolder, command.idset());
			if (allocatedIds.sourceSet.isBlank()) {
				// source uids are not available
				ctx.write(command.raw().tag() + " OK Source uids not found\r\n").onComplete(completed);
			} else {
				ctx.write(command.raw().tag() + " OK [COPYUID " + allocatedIds.targetUidValidity + " "
						+ allocatedIds.sourceSet + " " + allocatedIds.set() + "] Done\r\n").onComplete(completed);
			}
		} catch (Exception e) {
			ctx.write(command.raw().tag() + " NO Copy failed (" + e.getMessage() + ").\r\n").onComplete(completed);
		}
	}

	@Override
	public SelectedFolder modifiedFolder(AnalyzedCommand cmd, ImapContext ctx) {
		return ctx.mailbox().select(((UidCopyCommand) cmd).folder());
	}

}
