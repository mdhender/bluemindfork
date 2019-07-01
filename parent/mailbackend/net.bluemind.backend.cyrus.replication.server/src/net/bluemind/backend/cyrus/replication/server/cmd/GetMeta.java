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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.cyrus.replication.server.ReplicationFrame;
import net.bluemind.backend.cyrus.replication.server.ReplicationSession;
import net.bluemind.backend.cyrus.replication.server.Token;
import net.bluemind.backend.cyrus.replication.server.state.ReplicationException;
import net.bluemind.backend.mail.replica.api.MailboxSub;
import net.bluemind.backend.mail.replica.api.SieveScript;

public class GetMeta implements IAsyncReplicationCommand {

	private static final Logger logger = LoggerFactory.getLogger(GetMeta.class);

	public static class GetMetaResponse extends CommandResult {
		private final List<String> subs;
		private final List<SieveScript> sieves;

		public GetMetaResponse() {
			super(Status.OK, "success");
			subs = new LinkedList<>();
			sieves = new LinkedList<>();
		}

		public String responseString() {
			try {
				StringBuilder resp = new StringBuilder();

				if (!subs.isEmpty()) {
					resp.append("* LSUB (").append(String.join(" ", subs)).append(")\r\n");
				}
				for (SieveScript ss : sieves) {
					resp.append("* SIEVE ").append(ss.toParenObjectString()).append("\r\n");
				}

				String ok = super.responseString();
				resp.append(ok);
				return resp.toString();
			} catch (Exception t) {
				logger.error(t.getMessage(), t);
				throw ReplicationException.serverError(t);
			}
		}

		public void addSub(MailboxSub f) {
			subs.add(quoteIfNeeded(f.mboxName));
		}

		private String quoteIfNeeded(String mboxName) {
			if (mboxName.contains(" ")) {
				return "\"" + mboxName + "\"";
			} else {
				return mboxName;
			}
		}

		public void addSieve(SieveScript f) {
			sieves.add(f);
		}

	}

	private static class KnownStuff {
		Collection<MailboxSub> knownSubs;
		Collection<SieveScript> knownSieves;
	}

	@Override
	public CompletableFuture<CommandResult> doIt(ReplicationSession session, Token verbToken, ReplicationFrame frame) {
		String withVerb = verbToken.value();
		String user = withVerb.substring("GET META ".length());
		KnownStuff known = new KnownStuff();
		return session.state().subByUser(user).thenCompose(foundSubs -> {
			known.knownSubs = foundSubs;
			return session.state().sieveByUser(user);
		}).thenApply(foundSieves -> {
			known.knownSieves = foundSieves;

			GetMetaResponse resp = new GetMetaResponse();

			for (MailboxSub f : known.knownSubs) {
				resp.addSub(f);
			}
			for (SieveScript f : known.knownSieves) {
				resp.addSieve(f);
			}
			return (CommandResult) resp;
		}).exceptionally(ex -> {
			ReplicationException re = ReplicationException.cast(ex);
			if (re != null) {
				return re.asResult();
			} else {
				return CommandResult.error(ex);
			}
		});
	}

}
