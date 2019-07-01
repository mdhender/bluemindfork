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
import net.bluemind.backend.cyrus.replication.server.state.ReplicationState;
import net.bluemind.backend.cyrus.replication.server.state.SieveData;

/**
 * APPLY UNSIEVE %(USERID admin@blue-mind.loc FILENAME
 * 3552E7D7-411A-4FF4-96D0-C2F963E87F91.sieve.script)
 */
public class ApplyUnsieve implements IAsyncReplicationCommand {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(ApplyUnsieve.class);

	public ApplyUnsieve() {
	}

	public CompletableFuture<CommandResult> doIt(ReplicationSession session, Token t, ReplicationFrame frame) {
		String withVerb = t.value();
		String toReserve = withVerb.substring("APPLY UNSIEVE ".length());
		ParenObjectParser parser = ParenObjectParser.create();
		JsonObject parsed = parser.parse(toReserve).asObject();
		ReplicationState state = session.state();
		SieveData asSieveData = SieveData.of(parsed);
		return state.unsieve(asSieveData).thenApply(v -> CommandResult.success());
	}

}
