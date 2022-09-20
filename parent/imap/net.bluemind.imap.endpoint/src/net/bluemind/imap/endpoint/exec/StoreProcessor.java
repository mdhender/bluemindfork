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

import io.netty.buffer.Unpooled;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.cmd.RawImapCommand;
import net.bluemind.imap.endpoint.cmd.StoreCommand;
import net.bluemind.imap.endpoint.cmd.UidFetchCommand;
import net.bluemind.imap.endpoint.parsing.Part;
import net.bluemind.lib.vertx.Result;

public class StoreProcessor extends SelectedStateCommandProcessor<StoreCommand> {

	@Override
	public Class<StoreCommand> handledType() {
		return StoreCommand.class;
	}

	@Override
	protected void checkedOperation(StoreCommand command, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		ctx.mailbox().updateFlags(ctx.selected(), command.idset(), command.mode(), command.flags());
		if (command.silent()) {
			ctx.write(command.raw().tag() + " OK Completed\r\n");
			completed.handle(Result.success());
		} else {
			String asFetch = command.raw().tag() + " UID FETCH " + command.idset() + " (FLAGS)";
			RawImapCommand raw = new RawImapCommand(
					Collections.singletonList(Part.endOfCommand(Unpooled.wrappedBuffer(asFetch.getBytes()))));
			UidFetchCommand fetch = new UidFetchCommand(raw);
			UidFetchProcessor proc = new UidFetchProcessor();
			proc.checkedOperation(fetch, ctx, completed);
		}
	}

}
