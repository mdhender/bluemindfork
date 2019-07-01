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

import net.bluemind.backend.cyrus.replication.server.ReplicationFrame;
import net.bluemind.backend.cyrus.replication.server.ReplicationSession;
import net.bluemind.backend.cyrus.replication.server.Token;
import net.bluemind.backend.mail.replica.api.MailboxAnnotation;

/**
 * 
 * GET ANNOTATION
 * vagrant.vmw!domino^room^a8177012dabce540c1257cd0004bbb34_at_domino^res
 * 
 * GET ANNOTATION ex2016.vmw!user.tom
 * 
 * ANNOTATION %(MBOXNAME ex2016.vmw!user.tom ENTRY
 * /vendor/blue-mind/replication/id USERID tom@ex2016.vmw VALUE 43)
 *
 * 
 */
public class GetAnnotation implements IAsyncReplicationCommand {

	private static final Logger logger = LoggerFactory.getLogger(GetAnnotation.class);

	public static class GetAnnoResponse extends CommandResult {
		private final List<MailboxAnnotation> annos;

		public GetAnnoResponse() {
			super(Status.OK, "success");
			annos = new LinkedList<>();
		}

		public void add(MailboxAnnotation an) {
			annos.add(an);
		}

		public String responseString() {
			StringBuilder resp = new StringBuilder();

			for (MailboxAnnotation an : annos) {
				resp.append("* ANNOTATION ").append(an.toParenObjectString()).append("\r\n");
			}

			String ok = super.responseString();
			resp.append(ok);
			String ret = resp.toString();
			logger.info("Replied:\n{}", ret);
			return ret;
		}
	}

	public GetAnnotation() {
	}

	public CompletableFuture<CommandResult> doIt(ReplicationSession session, Token t, ReplicationFrame frame) {
		CompletableFuture<CommandResult> ret = new CompletableFuture<>();
		String withVerb = t.value();
		String mbox = withVerb.substring("GET ANNOTATION ".length());
		logger.info("Get ANNOTATION: {}", mbox);
		session.state().annotationsByMailbox(mbox).thenAccept(annotations -> {
			logger.info("Got annotations: {}", annotations);
			GetAnnoResponse resp = new GetAnnoResponse();
			for (MailboxAnnotation ma : annotations) {
				resp.add(ma);
			}
			ret.complete(resp);
		}).exceptionally(ex -> {
			logger.error(ex.getMessage(), ex);
			ret.complete(CommandResult.success());
			return null;
		});
		return ret;
	}

}
