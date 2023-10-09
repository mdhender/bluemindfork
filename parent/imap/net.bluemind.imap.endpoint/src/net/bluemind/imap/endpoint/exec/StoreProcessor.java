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

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.Unpooled;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.cmd.FetchCommand;
import net.bluemind.imap.endpoint.cmd.RawImapCommand;
import net.bluemind.imap.endpoint.cmd.StoreCommand;
import net.bluemind.imap.endpoint.locks.IFlagsCheckpoint;
import net.bluemind.imap.endpoint.locks.ISequenceReader;
import net.bluemind.imap.endpoint.parsing.Part;

/**
 * <pre>
 * . store 4 +flags permaCrap
 * * FLAGS (\Answered \Flagged \Draft \Deleted \Seen permaCrap)
 * * OK [PERMANENTFLAGS (\Answered \Flagged \Draft \Deleted \Seen permaCrap \*)] Ok
 * * 4 FETCH (FLAGS (permaCrap))
 * . OK Completed
 * </pre>
 */
public class StoreProcessor extends SelectedStateCommandProcessor<StoreCommand>
		implements ISequenceReader, IFlagsCheckpoint {

	private static final Logger logger = LoggerFactory.getLogger(StoreProcessor.class);

	@Override
	public Class<StoreCommand> handledType() {
		return StoreCommand.class;
	}

	@Override
	protected void checkedOperation(StoreCommand command, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		long newVersion = ctx.mailbox().updateFlags(ctx.selected(), command.idset(), command.mode(), command.flags());
		ctx.nexus().dispatchSequencesChanged(ctx.mailbox(), command.raw().tag(), ctx.selected().folder.uid, newVersion);

		if (command.silent()) {
			StringBuilder resp = new StringBuilder();
			checkpointFlags(logger, command.raw().tag() + " store", ctx, resp);
			ctx.write(resp.toString() + command.raw().tag() + " OK Completed\r\n").onComplete(completed);
		} else {
			String asFetch = command.raw().tag() + " FETCH " + command.idset().serializedSet + " (FLAGS)";
			RawImapCommand raw = new RawImapCommand(
					Collections.singletonList(Part.endOfCommand(Unpooled.wrappedBuffer(asFetch.getBytes()))));
			FetchCommand fetch = new FetchCommand(raw);
			FetchProcessor proc = new FetchProcessor();
			proc.checkedOperation(fetch, ctx, completed);
		}
	}

}
