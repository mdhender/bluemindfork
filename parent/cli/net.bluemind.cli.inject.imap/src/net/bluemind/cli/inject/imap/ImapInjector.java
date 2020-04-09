/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.cli.inject.imap;

import java.io.ByteArrayInputStream;
import java.util.concurrent.Semaphore;

import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.StoreClient;
import net.bluemind.network.topology.Topology;

public class ImapInjector extends MailExchangeInjector {

	public static class ImapTargetMailbox extends TargetMailbox {
		StoreClient sc;
		Semaphore lock;

		public ImapTargetMailbox(String email, String sid) {
			super(email, sid);
			this.sc = new StoreClient(Topology.get().any("mail/imap").value.address(), 1143, email, sid);
			this.lock = new Semaphore(1);
		}

		public boolean prepare() {
			return sc.login();
		}

		public void exchange(TargetMailbox from, byte[] emlContent) {
			try {
				lock.acquire();
			} catch (InterruptedException e1) {
				Thread.currentThread().interrupt();
				return;
			}
			try {
				int added = sc.append("INBOX", new ByteArrayInputStream(emlContent), new FlagsList());
				logger.debug("Added {} to {}", added, email);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				System.exit(1);
			} finally {
				lock.release();
			}
		}
	}

	public ImapInjector(IServiceProvider provider, String domainUid) {
		super(provider, domainUid, ImapTargetMailbox::new);

	}

}
