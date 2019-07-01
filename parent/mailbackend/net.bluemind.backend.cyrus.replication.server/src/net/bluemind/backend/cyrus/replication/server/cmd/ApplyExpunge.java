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

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.backend.cyrus.replication.protocol.parsing.JsUtils;
import net.bluemind.backend.cyrus.replication.protocol.parsing.ParenObjectParser;
import net.bluemind.backend.cyrus.replication.server.ReplicationFrame;
import net.bluemind.backend.cyrus.replication.server.ReplicationSession;
import net.bluemind.backend.cyrus.replication.server.Token;
import net.bluemind.backend.cyrus.replication.server.state.ReplicationException;

/**
 * APPLY EXPUNGE %(MBOXNAME blue-mind.net!user.estelle^martinez UNIQUEID
 * 2e5e20c75256a4d9 UID (16822))
 *
 */
public class ApplyExpunge implements IAsyncReplicationCommand {

	private static final Logger logger = LoggerFactory.getLogger(ApplyExpunge.class);

	public ApplyExpunge() {
	}

	public CompletableFuture<CommandResult> doIt(ReplicationSession session, Token t, ReplicationFrame frame) {
		CompletableFuture<CommandResult> ret = new CompletableFuture<>();
		String withVerb = t.value();
		String toReserve = withVerb.substring("APPLY EXPUNGE ".length());
		ParenObjectParser parser = ParenObjectParser.create();
		JsonObject parsed = parser.parse(toReserve).asObject();
		String mbox = parsed.getString("MBOXNAME");
		JsonArray uid = parsed.getArray("UID");
		List<Long> toExpunge = JsUtils.asList(uid, (String s) -> Long.parseLong(s));
		session.state().expunge(mbox, toExpunge).whenComplete((v, ex) -> {
			if (ex != null) {
				ReplicationException rs = ReplicationException.cast(ex);
				if (rs != null) {
					ret.complete(rs.asResult());
				} else {
					ret.completeExceptionally(ex);
				}
			} else {
				ret.complete(CommandResult.success());
			}
		});

		logger.info("Apply EXPUNGE from {} of {}", mbox, uid);

		return ret;
	}

}
