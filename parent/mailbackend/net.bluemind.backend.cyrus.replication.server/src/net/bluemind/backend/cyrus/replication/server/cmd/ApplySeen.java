/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.backend.cyrus.replication.server.cmd;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonObject;
import net.bluemind.backend.cyrus.replication.protocol.parsing.ParenObjectParser;
import net.bluemind.backend.cyrus.replication.server.ReplicationFrame;
import net.bluemind.backend.cyrus.replication.server.ReplicationSession;
import net.bluemind.backend.cyrus.replication.server.Token;
import net.bluemind.backend.mail.replica.api.SeenOverlay;

/**
 * APPLY SEEN %(USERID tom@vagrant.vmw UNIQUEID 6bef31d958676eb1 LASTREAD
 * 1483174216 LASTUID 1 LASTCHANGE 1483174214 SEENUIDS "")
 * 
 *
 */
public class ApplySeen implements IAsyncReplicationCommand {

	private static final Logger logger = LoggerFactory.getLogger(ApplySeen.class);

	public ApplySeen() {
	}

	public CompletableFuture<CommandResult> doIt(ReplicationSession session, Token t, ReplicationFrame frame) {
		CompletableFuture<CommandResult> ret = new CompletableFuture<>();
		String withVerb = t.value();
		String toReserve = withVerb.substring("APPLY SEEN ".length());
		ParenObjectParser parser = ParenObjectParser.create();
		JsonObject parsed = parser.parse(toReserve).asObject();
		SeenOverlay overlay = SeenOverlay.of(parsed);
		logger.info("Add SeenOverlay {}", overlay);
		session.state().seenOverlay(overlay).thenAccept(v -> ret.complete(CommandResult.success()));
		return ret;
	}

}
