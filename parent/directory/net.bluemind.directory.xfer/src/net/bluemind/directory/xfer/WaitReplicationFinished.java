/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.directory.xfer;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.imap.Annotation;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.StoreClient;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.Server;

public class WaitReplicationFinished {
	private static final Logger logger = LoggerFactory.getLogger(WaitReplicationFinished.class);

	private WaitReplicationFinished() {
	}

	public static CompletableFuture<Long> doProbe(Vertx vertx, Probe probe) {
		CompletableFuture<Long> replFeeback = new CompletableFuture<>();
		ItemValue<Server> backend = Topology.get().datalocation(probe.dataLocation);

		String backendAddress = backend.value.address();

		try (StoreClient sc = new StoreClient(backendAddress, 1143, probe.latd, probe.authKey)) {
			if (!sc.login()) {
				throw new IMAPException("Login failed for " + probe.latd + " (" + backendAddress + ")");
			}
			String mboxName = "INBOX";
			boolean selected = sc.select(mboxName);
			if (!selected) {
				throw new IMAPException("SELECT of " + mboxName + " (" + backendAddress + ") failed");
			}
			Annotation annot = sc.getAnnotation(mboxName, "/vendor/cmu/cyrus-imapd/uniqueid")
					.get("/vendor/cmu/cyrus-imapd/uniqueid");
			if (annot == null) {
				throw new IMAPException("/vendor/cmu/cyrus-imapd/uniqueid annotation is missing");
			}

			logger.info("Mailbox {} ({}) selected folder: {}, uniqueId: {}", probe.latd, backendAddress, selected,
					annot.valueShared);

			String eml = "From: waitreplication@bluemind.net\r\nSubject: PROBE\r\n\r\nprobed.\r\n\r\n";
			FlagsList fl = new FlagsList();
			fl.add(Flag.DELETED);
			int addedUid = sc.append(mboxName, new ByteArrayInputStream(eml.getBytes(StandardCharsets.US_ASCII)), fl);
			logger.info("Mailbox {} ({}) added uid: {}", probe.latd, backendAddress, addedUid);
			if (addedUid <= 0) {
				throw new IMAPException(
						"Failed to add a message to mailbox " + probe.latd + " (" + backendAddress + ")");
			}

			CompletableFuture<Void> applyMailboxPromise = ReplicationFeebackObserver.addWatcher(vertx,
					annot.valueShared, addedUid, 30, TimeUnit.MINUTES);

			long now = System.currentTimeMillis();
			applyMailboxPromise.whenComplete((v, ex) -> {
				if (ex != null) {
					replFeeback.completeExceptionally(ex);
				} else {
					replFeeback.complete(System.currentTimeMillis() - now);
				}
			});
		} catch (Exception ex) {
			replFeeback.completeExceptionally(ex);
		}
		return replFeeback;

	}
}
