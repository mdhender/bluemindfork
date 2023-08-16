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
import net.bluemind.common.vertx.contextlogging.ContextualData;
import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.SessionState;
import net.bluemind.imap.endpoint.cmd.AuthenticateCommand;
import net.bluemind.lib.vertx.Result;

public class AuthenticateProcessor implements CommandProcessor<AuthenticateCommand> {

	private static final Logger logger = LoggerFactory.getLogger(AuthenticateProcessor.class);

	@Override
	public void operation(AuthenticateCommand lc, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		ctx.state(SessionState.IN_AUTH);
		ContextualData.put("mech", lc.mech());
		ContextualData.put("auth_tag", lc.raw().tag());
		ctx.write("+\r\n");
		if (logger.isInfoEnabled()) {
			logger.info("auth with MECH {}", lc.mech());
		}

		completed.handle(Result.success());
	}

	@Override
	public Class<AuthenticateCommand> handledType() {
		return AuthenticateCommand.class;
	}

}
