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
import net.bluemind.imap.endpoint.ImapEndpointActivator;
import net.bluemind.imap.endpoint.cmd.IdCommand;
import net.bluemind.lib.vertx.Result;

public class IdProcessor extends AuthenticatedCommandProcessor<IdCommand> {

	private static final Logger logger = LoggerFactory.getLogger(IdProcessor.class);

	private static final String MY_ID = "* ID (\"name\" \"BlueMind\" \"version\" \""
			+ ImapEndpointActivator.getVersion() + "\")\r\n";

	@Override
	public void checkedOperation(IdCommand id, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		String resp = MY_ID + id.raw().tag() + " OK Completed\r\n";
		ctx.write(resp);
		logger.info("Id-ed myself as version {} to {}", ImapEndpointActivator.getVersion(), id.clientId().get("name"));
		ctx.clientId(id.clientId());
		completed.handle(Result.success());
	}

	@Override
	public Class<IdCommand> handledType() {
		return IdCommand.class;
	}

}
