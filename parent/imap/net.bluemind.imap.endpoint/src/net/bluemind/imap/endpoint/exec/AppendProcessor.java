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
import net.bluemind.imap.endpoint.cmd.AppendCommand;
import net.bluemind.imap.endpoint.driver.AppendStatus;
import net.bluemind.imap.endpoint.driver.AppendStatus.WriteStatus;
import net.bluemind.imap.endpoint.driver.SelectedFolder;
import net.bluemind.imap.endpoint.locks.ISequenceCheckpoint;
import net.bluemind.imap.endpoint.locks.ISequenceWriter;

/**
 * <code>A003 OK [APPENDUID 38505 3955] APPEND completed</code>.
 *
 * If target folder is the same as the selected one, we SHOULD return EXISTS
 * untagged response.
 *
 */
public class AppendProcessor extends AuthenticatedCommandProcessor<AppendCommand>
		implements ISequenceWriter, ISequenceCheckpoint {

	private static final Logger logger = LoggerFactory.getLogger(AppendProcessor.class);

	@Override
	protected void checkedOperation(AppendCommand command, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		SelectedFolder target = ctx.mailbox().select(command.folder());
		AppendStatus appendStatus = ctx.mailbox().append(target, command.flags(), command.deliveryDate(),
				command.buffer());
		StringBuilder resp = new StringBuilder();
		if (appendStatus.status() == WriteStatus.WRITTEN) {
			ctx.nexus().dispatchSequencesChanged(ctx.mailbox(), command.raw().tag(), target.folder.uid,
					target.contentVersion);
			if (appendingToSelectedFolder(target, ctx.selected())) {
				checkpointSequences(logger, command.raw().tag() + " append", resp, ctx);
			}
		}
		resp.append(command.raw().tag() + appendStatus.statusName() + "\r\n");
		ctx.write(resp.toString()).onComplete(completed);
	}

	private boolean appendingToSelectedFolder(SelectedFolder target, SelectedFolder currentSelection) {
		return target != null && currentSelection != null && target.folder.uid.equals(currentSelection.folder.uid);
	}

	@Override
	public SelectedFolder modifiedFolder(AnalyzedCommand cmd, ImapContext ctx) {
		return ctx.mailbox().select(((AppendCommand) cmd).folder());
	}
	
	@Override
	public Class<AppendCommand> handledType() {
		return AppendCommand.class;
	}

}
