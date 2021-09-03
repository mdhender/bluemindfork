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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

import com.github.javafaker.Beer;
import com.github.javafaker.Country;
import com.github.javafaker.Faker;
import com.google.common.base.Strings;

import net.bluemind.cli.inject.common.GOTMessageProducer;
import net.bluemind.cli.inject.common.IMessageProducer;
import net.bluemind.cli.inject.common.MailExchangeInjector;
import net.bluemind.cli.inject.common.TargetMailbox;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.StoreClient;
import net.bluemind.network.topology.Topology;

public class ImapInjector extends MailExchangeInjector {

	public static class ImapTargetMailbox extends TargetMailbox {
		StoreClient sc;
		Semaphore lock;
		private int folders;
		private List<String> target;
		private int bound;

		public ImapTargetMailbox(String email, String sid, int folders) {
			super(email, sid);
			this.sc = new StoreClient(Topology.get().any("mail/imap").value.address(), 1143, email, sid);
			this.lock = new Semaphore(1);
			this.target = new ArrayList<>(1 + folders * folders);
			this.target.add("INBOX");
			this.bound = 1;
			this.folders = folders;
		}

		public boolean prepare() {
			boolean ret = sc.login();
			if (ret && folders > 0) {
				try {
					Country country = Faker.instance().country();
					Beer beer = Faker.instance().beer();
					int lvl1 = folders;
					int lvl2 = folders;
					for (int i = 0; i < lvl1; i++) {
						String root = (Strings.padStart(Integer.toString(i), 3, '0') + " " + country.name())
								.replace(' ', '_');
						boolean created = sc.create(root);
						if (created) {
							target.add(root);
							for (int j = 0; j < lvl2; j++) {
								String sub = (root + "/" + Strings.padStart(Integer.toString(j), 3, '0') + " "
										+ beer.name()).replace(' ', '_');
								if (sc.create(sub)) {
									target.add(sub);
								}

							}
						}
					}
					this.bound = target.size();
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					ret = false;
				}
			}
			return ret;
		}

		public void exchange(TargetMailbox from, byte[] emlContent) {
			try {
				lock.acquire();
			} catch (InterruptedException e1) {
				Thread.currentThread().interrupt();
				return;
			}
			try {
				int added = sc.append(target.get(ThreadLocalRandom.current().nextInt(bound)),
						new ByteArrayInputStream(emlContent), new FlagsList());
				logger.debug("Added {} to {}", added, email);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			} finally {
				lock.release();
			}
		}
	}

	public ImapInjector(IServiceProvider provider, String domainUid, IMessageProducer prod, int folders) {
		super(provider, domainUid, (em, sid) -> new ImapTargetMailbox(em, sid, folders), prod);
	}

	public ImapInjector(IServiceProvider provider, String domainUid) {
		this(provider, domainUid, new GOTMessageProducer(), 5);
	}

}
