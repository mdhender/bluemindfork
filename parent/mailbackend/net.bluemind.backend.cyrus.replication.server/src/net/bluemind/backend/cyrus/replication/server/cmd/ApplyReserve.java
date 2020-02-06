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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonObject;
import net.bluemind.backend.cyrus.replication.protocol.parsing.JsUtils;
import net.bluemind.backend.cyrus.replication.protocol.parsing.ParenObjectParser;
import net.bluemind.backend.cyrus.replication.server.ReplicationFrame;
import net.bluemind.backend.cyrus.replication.server.ReplicationSession;
import net.bluemind.backend.cyrus.replication.server.Token;

/**
 * APPLY RESERVE %(PARTITION vagrant_vmw MBOXNAME (vagrant.vmw!user.admin) GUID
 * (0cd4d7a059b7b5772b33881da783536bf06020d7))
 * 
 *
 */
public class ApplyReserve implements IAsyncReplicationCommand {

	private static final Logger logger = LoggerFactory.getLogger(ApplyReserve.class);

	public ApplyReserve() {
	}

	public static class ApplyReserveResponse extends CommandResult {

		private LinkedList<String> missing;

		protected ApplyReserveResponse() {
			super(Status.OK, "success");
			this.missing = new LinkedList<>();
		}

		public void addMissing(String guid) {
			missing.add(guid);
		}

		public String responseString() {
			if (!missing.isEmpty()) {
				return "* MISSING (" + String.join(" ", missing) + ")\r\n" + super.responseString();
			} else {
				return super.responseString();
			}
		}

	}

	public CompletableFuture<CommandResult> doIt(ReplicationSession session, Token t, ReplicationFrame frame) {
		String withVerb = t.value();
		String toReserve = withVerb.substring("APPLY RESERVE ".length());
		ParenObjectParser parser = ParenObjectParser.create();
		JsonObject parsed = parser.parse(toReserve).asObject();
		if (logger.isDebugEnabled()) {
			logger.debug("Should RESERVE {}", parsed.encodePrettily());
		}
		List<String> guids = JsUtils.asList(parsed.getJsonArray("GUID"), (String s) -> s);
		String partition = parsed.getString("PARTITION");

		return session.state().missingGuids(partition, guids).thenApply(missing -> {
			ApplyReserveResponse resp = new ApplyReserveResponse();
			for (String miss : missing) {
				resp.addMissing(miss);
			}
			return resp;
		});
	}

}
