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
import net.bluemind.imap.endpoint.SessionState;
import net.bluemind.imap.endpoint.cmd.AnalyzedCommand;
import net.bluemind.lib.vertx.Result;

public abstract class SelectedStateCommandProcessor<T extends AnalyzedCommand>
		extends StateConstrainedCommandProcessor<T> {
	private static final Logger logger = LoggerFactory.getLogger(SelectedStateCommandProcessor.class);

	protected boolean stateCheck(T command, ImapContext ctx, Handler<AsyncResult<Void>> completed) {

		if (ctx.state() != SessionState.SELECTED) {
			if (ctx.state() == SessionState.NOT_AUTHENTICATED) {
				logger.warn("Rejecting {} in not authenticated state", command);
				ctx.write(command.raw().tag() + " BAD invalid command\r\n");
			} else {
				logger.warn("Selected state required for {}", command);
				ctx.write(command.raw().tag() + " BAD Please select a mailbox first\r\n");
			}
			completed.handle(Result.success());
			return false;
		}
		return true;
	}

}
