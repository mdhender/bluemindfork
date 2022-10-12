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

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.cmd.AnalyzedCommand;

public interface CommandProcessor<T extends AnalyzedCommand> {

	Class<T> handledType();

	default void process(AnalyzedCommand command, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		Class<T> type = handledType();
		T casted = type.cast(command);
		try {
			operation(casted, ctx, completed);
		} catch (Exception e) {
			// Uncatched exception: always respond
			ctx.write(command.raw().tag() + " NO unknown error: " + e.getMessage() + "\r\n");
			throw e;
		}
	}

	void operation(T command, ImapContext ctx, Handler<AsyncResult<Void>> completed);

}
