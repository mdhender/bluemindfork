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
import net.bluemind.lib.vertx.Result;

public class UidFetchProcessor extends SelectedStateCommandProcessor<UidFetchCommand> {

	private static final Logger logger = LoggerFactory.getLogger(UidFetchProcessor.class);

	@Override
	public Class<UidFetchCommand> handledType() {
		return UidFetchCommand.class;
	}

	@Override
	protected void checkedOperation(UidFetchCommand command, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		MailboxConnection con = ctx.mailbox();

		FetchedItemStream output = new FetchedItemStream(ctx, command.fetchSpec());
		logger.debug("Fetching to {}", output);
		Stopwatch chrono = Stopwatch.createStarted();
		con.fetch(ctx.selected(), command.idset(), command.fetchSpec(), output).thenAccept(v -> {
			long ms = chrono.elapsed(TimeUnit.MILLISECONDS);
			ctx.write(command.raw().tag() + " OK Completed (took " + ms + "ms)\r\n");
			completed.handle(Result.success());
		}).exceptionally(t -> {
			completed.handle(Result.fail(t));
			return null;
		});
	}

}
