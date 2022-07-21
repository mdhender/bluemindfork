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

import java.util.List;
import java.util.stream.Collectors;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.cmd.UidSearchCommand;
import net.bluemind.lib.vertx.Result;

public class UidSearchProcessor extends SelectedStateCommandProcessor<UidSearchCommand> {

	@Override
	public Class<UidSearchCommand> handledType() {
		return UidSearchCommand.class;
	}

	@Override
	protected void checkedOperation(UidSearchCommand command, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		List<Long> imapUids = ctx.mailbox().uids(ctx.selected(), command.query());
		if (imapUids.isEmpty()) {
			ctx.write("* SEARCH\r\n" + command.raw().tag() + " OK Completed\r\n");
		} else {
			String uidsResp = imapUids.stream().mapToLong(Long::longValue).mapToObj(Long::toString)
					.collect(Collectors.joining(" ", "* SEARCH ", "\r\n" + command.raw().tag() + " OK Completed\r\n"));
			ctx.write(uidsResp);
		}
		completed.handle(Result.success());

	}

}
