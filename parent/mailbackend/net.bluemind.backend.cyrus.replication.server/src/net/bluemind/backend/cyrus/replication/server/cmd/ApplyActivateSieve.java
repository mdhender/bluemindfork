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
import net.bluemind.backend.mail.replica.api.SieveScript;

/**
 * <pre>
 * APPLY SIEVE %(USERID user1536128310085@test1536128310085.lab FILENAME
 * user1536128310085.sieve.script LAST_UPDATE 1536128319 CONTENT {265+}{t2.bin})
 * </pre>
 * 
 * is followed by
 * 
 * <pre>
 * APPLY ACTIVATE_SIEVE %(USERID user1536128310085@test1536128310085.lab FILENAME user1536128310085.sieve.bc)
 * </pre>
 * 
 */
public class ApplyActivateSieve implements IAsyncReplicationCommand {

	private static final Logger logger = LoggerFactory.getLogger(ApplyActivateSieve.class);

	public ApplyActivateSieve() {
	}

	public CompletableFuture<CommandResult> doIt(ReplicationSession session, Token t, ReplicationFrame frame) {
		String withVerb = t.value();
		String toReserve = withVerb.substring("APPLY ACTIVATE_SIEVE ".length());
		ParenObjectParser parser = ParenObjectParser.create();
		JsonObject parsed = parser.parse(toReserve).asObject();
		String userName = parsed.getString("USERID");
		String fn = parsed.getString("FILENAME");
		ReplicationState state = session.state();
		return state.sieveByUser(userName).thenCompose(known -> {
			for (SieveScript ss : known) {
				if (ss.fileName.equals(fn)) {
					logger.info("Activate {}", ss.fileName);
					ss.isActive = true;
					return state.sieve(SieveData.of(ss));
				}
			}
			return CompletableFuture.completedFuture(null);
		}).thenApply(v -> {
			logger.info("Applied ACTIVATE_SIEVE");
			return CommandResult.success();
		});
	}

}
