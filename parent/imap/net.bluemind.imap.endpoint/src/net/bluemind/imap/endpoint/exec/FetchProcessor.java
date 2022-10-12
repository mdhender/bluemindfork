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
import net.bluemind.imap.endpoint.cmd.FetchCommand;
import net.bluemind.imap.endpoint.driver.MailboxConnection;
import net.bluemind.lib.vertx.Result;

public class FetchProcessor extends SelectedStateCommandProcessor<FetchCommand> {

	private static final Logger logger = LoggerFactory.getLogger(FetchProcessor.class);

	@Override
	public Class<FetchCommand> handledType() {
		return FetchCommand.class;
	}

	@Override
	protected void checkedOperation(FetchCommand command, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		MailboxConnection con = ctx.mailbox();

		FetchedItemStream output = new FetchedItemStream(ctx, command.fetchSpec());
		logger.debug("Fetching to {}", output);

		con.fetch(ctx.selected(), command.idset(), command.fetchSpec(), output).thenAccept(v -> {
			ctx.write(command.raw().tag() + " OK Completed\r\n");
			completed.handle(Result.success());
		}).exceptionally(t -> {
			ctx.write(command.raw().tag() + " NO unknown error: " + t.getMessage() + "\r\n");
			completed.handle(Result.fail(t));
			return null;
		});
	}

}
