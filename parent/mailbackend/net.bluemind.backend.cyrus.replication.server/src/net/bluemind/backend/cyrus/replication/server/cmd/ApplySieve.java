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
import net.bluemind.backend.cyrus.replication.server.state.ReplicationState;
import net.bluemind.backend.cyrus.replication.server.state.SieveData;

/**
 * APPLY SIEVE %(USERID tom@vagrant.vmw FILENAME
 * 1F0C0FAA-179A-4B04-8049-146990AA3DFE.sieve.script LAST_UPDATE 1483173510
 * CONTENT {274+}{tok1483206244294-7.bin})
 * 
 *
 */
public class ApplySieve implements IAsyncReplicationCommand {

	private static final Logger logger = LoggerFactory.getLogger(ApplySieve.class);

	public ApplySieve() {
	}

	public CompletableFuture<CommandResult> doIt(ReplicationSession session, Token t, ReplicationFrame frame) {
		String withVerb = t.value();
		String toReserve = withVerb.substring("APPLY SIEVE ".length());
		ParenObjectParser parser = ParenObjectParser.create();
		JsonObject parsed = parser.parse(toReserve).asObject();
		SieveData sieve = SieveData.of(parsed);
		logger.info("Apply SIEVE {}", sieve);
		SieveData compiled = sieve.compiled();
		ReplicationState state = session.state();
		// we save 2 scripts as the compiled version is the one that cyrus activates
		return state.sieve(sieve).thenCompose(v -> state.sieve(compiled)).thenApply(v -> CommandResult.success());
	}

}
