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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.cmd.UidExpungeCommand;
import net.bluemind.imap.endpoint.driver.UpdateMode;
import net.bluemind.lib.vertx.Result;

public class UidExpungeProcessor extends SelectedStateCommandProcessor<UidExpungeCommand> {

	private static final Logger logger = LoggerFactory.getLogger(UidExpungeProcessor.class);

	@Override
	public Class<UidExpungeCommand> handledType() {
		return UidExpungeCommand.class;
	}

	@Override
	protected void checkedOperation(UidExpungeCommand command, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		ctx.mailbox().updateFlags(ctx.selected(), command.idset(), UpdateMode.Add,
				Collections.singletonList("\\Expunged"));
		ctx.write(command.raw().tag() + " OK Completed\r\n");
		completed.handle(Result.success());
		if (logger.isDebugEnabled()) {
			logger.debug("Expunged {} in {}", command.idset(), ctx.selected());
		}

	}

}
