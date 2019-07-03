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

import net.bluemind.backend.cyrus.replication.server.ReplicationFrame;
import net.bluemind.backend.cyrus.replication.server.ReplicationSession;
import net.bluemind.backend.cyrus.replication.server.Token;
import net.bluemind.backend.cyrus.replication.server.state.ReplicationException;

/**
 * APPLY UNMAILBOX vagrant.vmw!user.admin.pouic
 *
 */
public class ApplyUnmailbox implements IAsyncReplicationCommand {

	private static final Logger logger = LoggerFactory.getLogger(ApplyUnmailbox.class);

	public ApplyUnmailbox() {
	}

	public CompletableFuture<CommandResult> doIt(ReplicationSession session, Token t, ReplicationFrame frame) {
		String withVerb = t.value();
		String toDelete = withVerb.substring("APPLY UNMAILBOX ".length());
		return session.state().delete(toDelete).thenApply(v -> {
			logger.info("deleted {}", toDelete);
			return CommandResult.success();
		}).exceptionally(ex -> {
			ReplicationException rs = ReplicationException.cast(ex);
			if (rs != null) {
				logger.error("deletion of missing object ?", rs);
				return rs.asResult();
			} else {
				logger.error("Unmailbox OK even with {}", ex.getMessage());
				return CommandResult.success();
			}
		});
	}

}
