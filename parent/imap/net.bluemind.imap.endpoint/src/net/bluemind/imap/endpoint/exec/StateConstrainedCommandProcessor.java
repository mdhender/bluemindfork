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

public abstract class StateConstrainedCommandProcessor<T extends AnalyzedCommand> implements CommandProcessor<T> {
	private static final Logger logger = LoggerFactory.getLogger(StateConstrainedCommandProcessor.class);

	@Override
	public final void operation(T command, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		if (stateCheck(command, ctx, completed)) {
			checkedOperation(command, ctx, completed);
		} else {
			logger.warn("State check failed for {}", command);
		}
	}

	protected abstract boolean stateCheck(T command, ImapContext ctx, Handler<AsyncResult<Void>> completed);

	protected abstract void checkedOperation(T command, ImapContext ctx, Handler<AsyncResult<Void>> completed);

}
