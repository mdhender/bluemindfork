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

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import net.bluemind.backend.cyrus.replication.protocol.parsing.ParenObjectParser;
import net.bluemind.backend.cyrus.replication.server.ReplicationFrame;
import net.bluemind.backend.cyrus.replication.server.ReplicationSession;
import net.bluemind.backend.cyrus.replication.server.Token;
import net.bluemind.backend.cyrus.replication.server.state.ReplicationException.ErrorKind;
import net.bluemind.backend.cyrus.replication.server.state.ReplicationState;

/**
 * 
 * Successful response :
 * 
 * GET FETCH %(MBOXNAME devenv.blue!user.tom PARTITION bm-master__devenv_blue
 * UNIQUEID 1869a025-ccb7-4ce6-a747-00372aee447b GUID
 * a28170b7610c9cee6a217d5c3ced5cbddd0b0562 UID 5)
 * 
 * x MESSAGE %{bm-master__devenv_blue a28170b7610c9cee6a217d5c3ced5cbddd0b0562
 * 1235}
 * 
 * THE_EML
 * 
 * OK success
 * 
 * 
 * 
 * 
 */
public class GetFetch implements IAsyncReplicationCommand {

	private static final Logger logger = LoggerFactory.getLogger(GetFetch.class);

	public GetFetch() {
	}

	public CompletableFuture<CommandResult> doIt(ReplicationSession session, Token t, ReplicationFrame frame) {
		String withVerb = t.value();
		String toFetch = withVerb.substring("GET FETCH ".length());
		JsonObject recordJs = ParenObjectParser.create().parse(toFetch).asObject();
		logger.info("Should fetch {}", recordJs);
		ReplicationState state = session.state();
		String bodyGuid = recordJs.getString("GUID");
		return state.folderByName(recordJs.getString("MBOXNAME")).thenCompose(folder -> {
			return state.record(folder, bodyGuid, Long.parseLong(recordJs.getString("UID")));
		}).thenApply(optBuf -> {
			return optBuf.map(buf -> {
				logger.info("Success response with EML ({} byte(s))", buf.length());
				Buffer fullResp = Buffer.buffer();
				fullResp.appendString("* MESSAGE %{" + recordJs.getString("PARTITION") + " " + bodyGuid + " "
						+ buf.length() + "}\r\n");
				fullResp.appendBuffer(buf);
				fullResp.appendString("\r\nOK success\r\n");
				return CommandResult.fromBuffer(fullResp);
			}).orElse(ErrorKind.mailboxNonExistent.result());
		});
	}

}
