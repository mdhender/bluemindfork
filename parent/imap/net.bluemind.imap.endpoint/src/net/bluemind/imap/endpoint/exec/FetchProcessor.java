/* BEGIN LICENSE00
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

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.cmd.FetchCommand;
import net.bluemind.imap.endpoint.driver.MailboxConnection;
import net.bluemind.imap.endpoint.locks.IFlagsCheckpoint;
import net.bluemind.imap.endpoint.locks.ISequenceReader;
import net.bluemind.lib.vertx.Result;

public class FetchProcessor extends SelectedStateCommandProcessor<FetchCommand>
		implements ISequenceReader, IFlagsCheckpoint {

	private static final Logger logger = LoggerFactory.getLogger(FetchProcessor.class);

	@Override
	protected void checkedOperation(FetchCommand command, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		checkedOperation(command, ctx, Stopwatch.createStarted(), completed);
	}

	protected void checkedOperation(FetchCommand command, ImapContext ctx, Stopwatch chrono,
			Handler<AsyncResult<Void>> completed) {
		MailboxConnection con = ctx.mailbox();

		FetchedItemStream output = new FetchedItemStream(ctx, command.raw().tag(), command.fetchSpec());
		if (logger.isDebugEnabled()) {
			logger.debug("{} Fetching to {}", command.raw().tag(), output);
			logger.debug("{} knows {}: {}", command.raw().tag(), ctx.selected().sequences.length,
					ctx.selected().sequences);
			logger.debug("{} knows labels {}", command.raw().tag(), ctx.selected().labels);
		}

		StringBuilder sb = new StringBuilder();
		checkpointFlags(logger, command.raw().tag() + " fetch", ctx, sb);
		ctx.write(sb.toString());

		con.fetch(ctx.selected(), command.idset(), command.fetchSpec(), output).thenAccept(v -> {
			long ms = chrono.elapsed(TimeUnit.MILLISECONDS);
			ctx.write(command.raw().tag() + " OK Completed (took " + ms + " ms)\r\n").onComplete(completed);
		}).exceptionally(t -> {
			ctx.write(command.raw().tag() + " NO unknown error: " + t.getMessage() + "\r\n")
					.onComplete(writeAr -> completed.handle(Result.fail(t)));
			return null;
		});
	}
	
	@Override
	public Class<FetchCommand> handledType() {
		return FetchCommand.class;
	}

}
