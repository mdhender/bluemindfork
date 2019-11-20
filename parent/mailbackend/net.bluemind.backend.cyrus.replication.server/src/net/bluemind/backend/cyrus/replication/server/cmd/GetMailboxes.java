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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonArray;

import net.bluemind.backend.cyrus.replication.protocol.parsing.ParenObjectParser;
import net.bluemind.backend.cyrus.replication.server.ReplicationFrame;
import net.bluemind.backend.cyrus.replication.server.ReplicationSession;
import net.bluemind.backend.cyrus.replication.server.Token;
import net.bluemind.backend.cyrus.replication.server.state.MailboxFolder;
import net.bluemind.backend.cyrus.replication.server.state.ReplicationState;

/**
 *
 * Request:
 * 
 * <pre>
 * C: GET MAILBOXES (vagrant.vmw!user.admin)
 * </pre>
 *
 * Reponse:
 * 
 * <pre>
 * S: * MAILBOX %(UNIQUEID 5596488a58661ddc MBOXNAME vagrant.vmw!user.admin LAST_UID 1 HIGHESTMODSEQ 10 RECENTUID 1 RECENTTIME 1483104873 LAST_APPENDDATE 1483088316 POP3_LAST_LOGIN 0 UIDVALIDITY 1483087324 PARTITION vagrant_vmw ACL "admin@vagrant.vmw	lrswipkxtecda	admin0	lrswipkxtecda	" OPTIONS P SYNC_CRC 3758469704)
 * S: OK success
 * </pre>
 *
 */
public class GetMailboxes implements IAsyncReplicationCommand {

	private static final Logger logger = LoggerFactory.getLogger(GetMailboxes.class);

	public GetMailboxes() {
	}

	public static class GetMailboxesResponse extends CommandResult {
		private List<MailboxFolder> folders;

		public GetMailboxesResponse() {
			super(Status.OK, "success");
			folders = new LinkedList<>();
		}

		public void addMailbox(MailboxFolder f) {
			folders.add(f);
		}

		public String responseString() {
			StringBuilder resp = new StringBuilder();
			for (MailboxFolder mf : folders) {
				resp.append("* MAILBOX ").append(mf.toParenObjectString()).append("\r\n");
			}
			String ok = super.responseString();
			resp.append(ok);
			logger.debug("Returning {} mailbox(es)", folders.size());
			return resp.toString();
		}
	}

	public CompletableFuture<CommandResult> doIt(ReplicationSession session, Token t, ReplicationFrame frame) {
		String withVerb = t.value();
		String mboxAndContent = withVerb.substring("GET MAILBOXES ".length());
		ParenObjectParser parser = ParenObjectParser.create();
		JsonArray requestedMboxes = parser.parse(mboxAndContent).asArray();
		if (logger.isDebugEnabled()) {
			logger.debug("Cyrus server ({}) wants our vision on {} mailbox(es)", session.remoteIp(),
					requestedMboxes.size());
		}
		GetMailboxesResponse response = new GetMailboxesResponse();
		ReplicationState state = session.state();
		int len = requestedMboxes.size();
		long start = System.currentTimeMillis();
		List<String> boxNames = new ArrayList<>(requestedMboxes.size());
		for (int i = 0; i < len; i++) {
			String mboxName = Token.atomOrValue(requestedMboxes.get(i));
			boxNames.add(mboxName);
		}
		return state.foldersByName(boxNames).thenApply(folders -> {
			folders.forEach(response::addMailbox);
			long elapsed = System.currentTimeMillis() - start;
			if (elapsed > 400) {
				logger.warn("Slow resolution of {} mailbox(es) in {}ms.", requestedMboxes.size(), elapsed);
			}
			return response;
		});
	}

}
