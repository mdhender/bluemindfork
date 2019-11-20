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
import net.bluemind.backend.cyrus.replication.server.state.MailboxFolder;
import net.bluemind.backend.cyrus.replication.server.state.ReplicationException;
import net.bluemind.backend.cyrus.replication.server.utils.MailboxNameHelper;
import net.bluemind.backend.mail.replica.api.MailboxSub;
import net.bluemind.backend.mail.replica.api.QuotaRoot;
import net.bluemind.backend.mail.replica.api.SeenOverlay;
import net.bluemind.backend.mail.replica.api.SieveScript;

public class GetUser implements IAsyncReplicationCommand {

	private static final Logger logger = LoggerFactory.getLogger(GetUser.class);

	public static class GetUserResponse extends CommandResult {

		private final List<MailboxFolder> folders;
		private final List<SeenOverlay> seens;
		private final List<QuotaRoot> quotas;
		private final List<String> subs;
		private final List<SieveScript> sieves;

		public GetUserResponse() {
			super(Status.OK, "success");
			folders = new LinkedList<>();
			seens = new LinkedList<>();
			quotas = new LinkedList<>();
			subs = new LinkedList<>();
			sieves = new LinkedList<>();
		}

		public void addMailbox(MailboxFolder f) {
			folders.add(f);
		}

		public String responseString() {
			try {
				StringBuilder resp = new StringBuilder();
				for (MailboxFolder mf : folders) {
					resp.append("* MAILBOX ").append(mf.toParenObjectString()).append("\r\n");
				}
				for (SeenOverlay seen : seens) {
					resp.append("* SEEN ").append(seen.toParenObjectString()).append("\r\n");
				}
				for (QuotaRoot seen : quotas) {
					resp.append("* QUOTA ").append(seen.toParenObjectString()).append("\r\n");
				}
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
			subs.add(MailboxNameHelper.quoteIfNeeded(f.mboxName));
		}

		public void addSieve(SieveScript f) {
			sieves.add(f);
		}

		public void addSeen(SeenOverlay so) {
			seens.add(so);
		}

		public void addQuota(QuotaRoot qr) {
			quotas.add(qr);
		}

	}

	private static class KnownStuff {
		List<MailboxFolder> knownFolders;
		Collection<SeenOverlay> knownSeens;
		Collection<QuotaRoot> knownQuotas;
		Collection<MailboxSub> knownSubs;
		Collection<SieveScript> knownSieves;
	}

	@Override
	public CompletableFuture<CommandResult> doIt(ReplicationSession session, Token verbToken, ReplicationFrame frame) {
		String withVerb = verbToken.value();
		String user = withVerb.substring("GET USER ".length());
		KnownStuff known = new KnownStuff();

		return session.state().foldersByUser(user).thenCompose(foundFolders -> {
			known.knownFolders = foundFolders;
			return session.state().seenOverlayByUser(user);
		}).thenCompose(foundOverlays -> {
			known.knownSeens = foundOverlays;
			return session.state().quotaByUser(user);
		}).thenCompose(foundQuotas -> {
			known.knownQuotas = foundQuotas;
			return session.state().subByUser(user);
		}).thenCompose(foundSubs -> {
			known.knownSubs = foundSubs;
			return session.state().sieveByUser(user);
		}).thenApply(foundSieves -> {
			known.knownSieves = foundSieves;

			GetUserResponse resp = new GetUserResponse();
			for (MailboxFolder f : known.knownFolders) {
				resp.addMailbox(f);
			}
			for (SeenOverlay so : known.knownSeens) {
				resp.addSeen(so);
			}
			for (QuotaRoot qr : known.knownQuotas) {
				resp.addQuota(qr);
			}
			for (MailboxSub f : known.knownSubs) {
				resp.addSub(f);
			}
			for (SieveScript f : known.knownSieves) {
				resp.addSieve(f);
			}
			return (CommandResult) resp;
		}).exceptionally(t -> {
			ReplicationException re = ReplicationException.cast(t);
			if (re != null && re.errorKind() == ReplicationException.ErrorKind.malformedMailboxName) {
				// this happens when we receive an unqualified mailbox name
				// cyrus sync_server returns ok to this
				logger.warn("Empty success response for missing user.");
				return CommandResult.success();
			} else if (re != null) {
				return re.asResult();
			} else {
				return CommandResult.error(t);
			}
		});
	}

}
