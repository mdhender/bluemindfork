/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.cyrus.replication.server.cmd;

import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.buffer.Buffer;

import net.bluemind.backend.cyrus.replication.server.ReplicationFrame;
import net.bluemind.backend.cyrus.replication.server.ReplicationSession;
import net.bluemind.backend.cyrus.replication.server.Token;

/**
 * 
 * AUTHENTICATE PLAIN
 * AGFkbWluMABiMWJhNjdmOC1lN2Y0LTQ4NTAtOGE3ZS0xMDIzMjczM2Y0NWU=
 * 
 * 
 */
public class Authenticate implements IAsyncReplicationCommand {

	private static final Logger logger = LoggerFactory.getLogger(Authenticate.class);

	@Override
	public CompletableFuture<CommandResult> doIt(ReplicationSession session, Token verbToken, ReplicationFrame frame) {
		CompletableFuture<CommandResult> ret = new CompletableFuture<>();
		String base64Creds = verbToken.value().substring("AUTHENTICATE PLAIN ".length());
		byte[] decoded = Base64.getDecoder().decode(base64Creds);
		List<String> creds = new LinkedList<>();
		Buffer cur = new Buffer();
		for (int i = 1; i < decoded.length; i++) {
			if (decoded[i] == 0) {
				creds.add(cur.toString());
				cur = new Buffer();
			} else {
				cur.appendByte(decoded[i]);
			}
		}
		if (cur.length() > 0) {
			creds.add(cur.toString());
		}
		logger.debug("Extracted {}", creds);
		if (creds.size() != 2) {
			logger.error("Credentials extraction failed for replication session.");
			ret.complete(CommandResult.no("way."));
		} else {
			Iterator<String> it = creds.iterator();
			String login = it.next();
			String pass = it.next();
			if (login.indexOf('@') < 0) {
				login = login + "@global.virt";
			}
			final String finalLogin = login;
			session.state().checkCredentials(login, pass).thenAccept(b -> {
				if (b) {
					logger.info("Replication session authenticated as '{}'.", finalLogin);
					ret.complete(CommandResult.success());
				} else {
					logger.error("Failed to authenticate replication session.");
					ret.complete(CommandResult.no("way."));
				}
			});
		}
		return ret;
	}

}
