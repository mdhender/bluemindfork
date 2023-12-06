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

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.cmd.UidFetchCommand;
import net.bluemind.imap.endpoint.driver.MailboxConnection;
import net.bluemind.imap.endpoint.locks.ISequenceCheckpoint;
import net.bluemind.imap.endpoint.locks.ISequenceReader;
import net.bluemind.lib.vertx.Result;

public class UidFetchProcessor extends SelectedStateCommandProcessor<UidFetchCommand>
		implements ISequenceReader, ISequenceCheckpoint {

	private static final Logger logger = LoggerFactory.getLogger(UidFetchProcessor.class);

	@Override
	public Class<UidFetchCommand> handledType() {
		return UidFetchCommand.class;
	}

	@Override
	protected void checkedOperation(UidFetchCommand command, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		checkedOperation(command, ctx, Stopwatch.createStarted(), completed);
	}

	protected void checkedOperation(UidFetchCommand command, ImapContext ctx, Stopwatch chrono,
			Handler<AsyncResult<Void>> completed) {
		MailboxConnection con = ctx.mailbox();

		FetchedItemStream output = new FetchedItemStream(ctx, command.raw().tag() + " uid fetch", command.fetchSpec());
		logger.debug("Fetching to {}", output);

		StringBuilder sb = new StringBuilder();
		checkpointSequences(logger, command.raw().tag() + " uid fetch", sb, ctx);
		ctx.write(sb.toString());

		con.fetch(ctx.selected(), command.idset(), command.fetchSpec(), output).thenAccept(v -> {
			long ms = chrono.elapsed(TimeUnit.MILLISECONDS);
			ctx.write(command.raw().tag() + " OK Completed (took " + ms + "ms)\r\n").onComplete(completed);
		}).exceptionally(t -> {
			ctx.write(command.raw().tag() + " NO unknown error: " + t.getMessage() + "\r\n")
					.onComplete(writeAr -> completed.handle(Result.fail(t)));
			return null;
		});
	}

}
