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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.common.vertx.contextlogging.ContextualData;
import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.SessionState;
import net.bluemind.imap.endpoint.cmd.AuthenticatePlainCommand;
import net.bluemind.imap.endpoint.driver.Drivers;
import net.bluemind.imap.endpoint.driver.MailboxConnection;
import net.bluemind.imap.endpoint.driver.MailboxDriver;
import net.bluemind.lib.vertx.Result;

public class AuthenticatePlainProcessor extends StateConstrainedCommandProcessor<AuthenticatePlainCommand> {

	private static final Logger logger = LoggerFactory.getLogger(AuthenticatePlainProcessor.class);
	private final Capabilities caps;

	public AuthenticatePlainProcessor() {
		this.caps = new Capabilities();
	}

	@Override
	public void checkedOperation(AuthenticatePlainCommand lc, ImapContext ctx, Handler<AsyncResult<Void>> completed) {
		String tag = ContextualData.get("auth_tag");
		logger.info("{} PLAIN for {}", tag, lc);

		ByteBuf authBuf = Unpooled.wrappedBuffer(lc.payload());
		int i;
		List<String> parts = new ArrayList<>(3);
		for (i = 0; i < authBuf.readableBytes(); i++) {
			if (authBuf.getByte(i) == 0x00) {
				parts.add(authBuf.readSlice(i).toString(StandardCharsets.US_ASCII));
				authBuf.skipBytes(1);
				i = 0;
			}
		}
		if (authBuf.readableBytes() > 0) {
			parts.add(authBuf.toString(StandardCharsets.US_ASCII));
		}
		if (parts.size() == 3) {
			String login = parts.get(1);
			String pass = parts.get(2);
			if (logger.isDebugEnabled()) {
				logger.debug("l: {}, p: {}", login, pass);
			}

			MailboxDriver driver = Drivers.activeDriver();
			MailboxConnection connection = driver.open(login, pass);
			if (connection != null) {
				ctx.mailbox(connection);
				ctx.state(SessionState.AUTHENTICATED);
				ctx.write(tag + " OK [CAPABILITY " + caps.all() + "] User logged in.\r\n");
				completed.handle(Result.success());
			} else {
				logger.warn("Delay NO response to {} login attempt.", login);
				ctx.vertx().setTimer(3000, tid -> {
					ctx.state(SessionState.NOT_AUTHENTICATED);
					ctx.write(tag + " NO bad login or password.\r\n");
					completed.handle(Result.success());
				});
			}

		} else {
			ctx.write(tag + " BAD broken payload without 3 parts\r\n");
			ctx.state(SessionState.NOT_AUTHENTICATED);
			completed.handle(Result.success());
		}
	}

	@Override
	public Class<AuthenticatePlainCommand> handledType() {
		return AuthenticatePlainCommand.class;
	}

	@Override
	protected boolean stateCheck(AuthenticatePlainCommand command, ImapContext ctx,
			Handler<AsyncResult<Void>> completed) {
		return ctx.state() == SessionState.IN_AUTH;
	}

}
