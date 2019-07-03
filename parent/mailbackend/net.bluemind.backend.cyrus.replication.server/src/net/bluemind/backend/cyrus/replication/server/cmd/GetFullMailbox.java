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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.cyrus.replication.protocol.parsing.ParenObjectParser;
import net.bluemind.backend.cyrus.replication.server.ReplicationFrame;
import net.bluemind.backend.cyrus.replication.server.ReplicationSession;
import net.bluemind.backend.cyrus.replication.server.Token;
import net.bluemind.backend.cyrus.replication.server.state.MailboxFolder;
import net.bluemind.backend.cyrus.replication.server.state.MboxRecord;
import net.bluemind.backend.cyrus.replication.server.state.ReplicationState;

/**
 *
 * Request:
 * 
 * <pre>
 * C: GET FULLMAILBOX vagrant.vmw!user.admin
 * </pre>
 * 
 * GET FULLMAILBOX "ex2016.vmw!user.tom.Deleted Messages"
 *
 * Response:
 * 
 * <pre>
 * * MAILBOX %(UNIQUEID 5596488a5890990f MBOXNAME vagrant.vmw!user.admin LAST_UID 3 HIGHESTMODSEQ 40 RECENTUID 3 RECENTTIME 1486035958 LAST_APPENDDATE 1485871655 POP3_LAST_LOGIN 0 UIDVALIDITY 1485871375 PARTITION vagrant_vmw ACL "admin@vagrant.vmw	lrswipkxtecda	admin0	lrswipkxtecda	" OPTIONS P SYNC_CRC 3635576038 RECORD (
 * %(UID 1 MODSEQ 36 LAST_UPDATED 1486035945 FLAGS (\Seen) INTERNALDATE 1485871611 SIZE 1281 GUID 065ba5d1f867100b0da8622f75ff7760d26c5a83) 
 * %(UID 2 MODSEQ 39 LAST_UPDATED 1486035950 FLAGS (\Flagged) INTERNALDATE 1485871635 SIZE 35189 GUID 1a8b394b6827ac783ad40bb0c293ce5822b9e0cf) 
 * %(UID 3 MODSEQ 40 LAST_UPDATED 1486035958 FLAGS (\Seen) INTERNALDATE 1485871655 SIZE 4376698 GUID 8f6a929e05aeee58dcf2e8289320572ee99a0a82))) 
 * OK success
 * </pre>
 *
 */
public class GetFullMailbox implements IAsyncReplicationCommand {

	private static final Logger logger = LoggerFactory.getLogger(GetFullMailbox.class);

	public GetFullMailbox() {
	}

	public static class GetFullResponse extends CommandResult {
		private MailboxFolder folder;
		private List<MboxRecord> records;

		public GetFullResponse() {
			super(Status.OK, "success");
		}

		public void setMailbox(MailboxFolder f) {
			folder = f;
		}

		public void setRecords(List<MboxRecord> recs) {
			this.records = recs;
			Collections.sort(this.records, (r1, r2) -> Long.compare(r1.uid(), r2.uid()));
			logger.debug("{} used in response", recs.size());
		}

		public String responseString() {
			StringBuilder resp = new StringBuilder();
			resp.append("* MAILBOX ").append(folder.toParenObjectString(records)).append("\r\n");
			String ok = super.responseString();
			resp.append(ok);
			return resp.toString();
		}

	}

	public CompletableFuture<CommandResult> doIt(ReplicationSession session, Token t, ReplicationFrame frame) {
		String withVerb = t.value();
		String mboxName = withVerb.substring("GET FULLMAILBOX ".length());
		ParenObjectParser pop = ParenObjectParser.create();
		mboxName = pop.parse("(" + mboxName + ")").asArray().get(0);
		ReplicationState state = session.state();
		GetFullResponse resp = new GetFullResponse();
		final String fBoxName = mboxName;
		return state.folderByName(fBoxName).thenCompose(known -> {
			if (known != null) {
				resp.setMailbox(known);
				return loadRecords(state, resp, known);
			} else {
				logger.warn("GET FULLMAILBOX on unknown folder {}", fBoxName);
				return CompletableFuture.completedFuture(CommandResult.success());
			}
		});
	}

	private CompletableFuture<CommandResult> loadRecords(ReplicationState state, GetFullResponse resp,
			MailboxFolder known) {
		return state.records(known).thenApply(recs -> {
			resp.setRecords(recs);
			return resp;
		});

	}

}
