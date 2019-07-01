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
import net.bluemind.backend.mail.replica.api.MailboxAnnotation;

/**
 * APPLY ANNOTATION %(MBOXNAME
 * vagrant.vmw!domino^room^00c86c6c2095945ec1257cc20052c6fa_at_domino^res ENTRY
 * /vendor/cmu/cyrus-imapd/sieve USERID "" VALUE
 * domino.room.00c86c6c2095945ec1257cc20052c6fa_at_domino.res.sieve)
 * 
 * APPLY ANNOTATION %(MBOXNAME test1509110340662.lab!user.user1509110340662
 * ENTRY /vendor/blue-mind/replication/id USERID
 * user1509110340662@test1509110340662.lab VALUE 42)
 *
 */
public class ApplyAnnotation implements IAsyncReplicationCommand {

	private static final Logger logger = LoggerFactory.getLogger(ApplyAnnotation.class);

	public ApplyAnnotation() {
	}

	public CompletableFuture<CommandResult> doIt(ReplicationSession session, Token t, ReplicationFrame frame) {
		CompletableFuture<CommandResult> ret = new CompletableFuture<>();

		String withVerb = t.value();
		String mboxAndContent = withVerb.substring("APPLY ANNOTATION ".length());
		ParenObjectParser parser = ParenObjectParser.create();
		JsonObject annotation = parser.parse(mboxAndContent).asObject();
		logger.info("Parsed annotation: {}", annotation.encodePrettily());
		MailboxAnnotation ma = MailboxAnnotation.of(annotation);
		ma.mailbox = Token.atomOrValue(ma.mailbox);
		ma.value = Token.atomOrValue(ma.value);
		session.state().annotate(ma).thenAccept(v -> ret.complete(CommandResult.success()));
		return ret;
	}

}
