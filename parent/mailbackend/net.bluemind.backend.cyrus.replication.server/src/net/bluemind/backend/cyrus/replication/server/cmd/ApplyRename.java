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
import org.vertx.java.core.json.JsonObject;

import net.bluemind.backend.cyrus.replication.protocol.parsing.ParenObjectParser;
import net.bluemind.backend.cyrus.replication.server.ReplicationFrame;
import net.bluemind.backend.cyrus.replication.server.ReplicationSession;
import net.bluemind.backend.cyrus.replication.server.Token;

/**
 * APPLY RENAME %(OLDMBOXNAME vagrant.vmw!user.admin.tictac NEWMBOXNAME
 * vagrant.vmw!user.admin.touctouc PARTITION vagrant_vmw)
 *
 */
public class ApplyRename implements IAsyncReplicationCommand {

	private static final Logger logger = LoggerFactory.getLogger(ApplyRename.class);

	public ApplyRename() {
	}

	public CompletableFuture<CommandResult> doIt(ReplicationSession session, Token t, ReplicationFrame frame) {
		CompletableFuture<CommandResult> ret = new CompletableFuture<>();
		String withVerb = t.value();
		String toReserve = withVerb.substring("APPLY RENAME ".length());
		ParenObjectParser parser = ParenObjectParser.create();
		JsonObject parsed = parser.parse(toReserve).asObject();
		String from = Token.atomOrValue(parsed.getString("OLDMBOXNAME"));
		String to = Token.atomOrValue(parsed.getString("NEWMBOXNAME"));
		logger.info("Apply RENAME from {} to {}", from, to);
		session.state().rename(from, to).whenComplete((v, ex) -> {
			if (ex != null) {
				logger.error("rename pb: " + ex.getMessage(), ex);
			}
			ret.complete(CommandResult.success());
		});
		return ret;
	}

}
