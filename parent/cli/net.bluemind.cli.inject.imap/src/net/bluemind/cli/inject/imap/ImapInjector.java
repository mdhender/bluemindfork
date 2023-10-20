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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

import com.google.common.base.Strings;

import net.bluemind.cli.inject.common.GOTMessageProducer;
import net.bluemind.cli.inject.common.IMessageProducer;
import net.bluemind.cli.inject.common.MailExchangeInjector;
import net.bluemind.cli.inject.common.TargetMailbox;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.StoreClient;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.datafaker.Faker;
import net.datafaker.providers.base.Country;
import net.datafaker.providers.food.Beer;

public class ImapInjector extends MailExchangeInjector {

	public static class ImapTargetMailbox extends TargetMailbox {
		StoreClient sc;
		Semaphore lock;
		private int folders;
		private List<String> target;
		private int bound;

		public ImapTargetMailbox(TargetMailbox.Auth auth, int folders) {
			super(auth);
			this.sc = new StoreClient(Topology.get().any(TagDescriptor.bm_core.getTag()).value.address(), 1143,
					auth.email(), auth.sid());
			this.lock = new Semaphore(1);
			this.target = new ArrayList<>(1 + folders * folders);
			this.target.add("INBOX");
			this.bound = 1;
			this.folders = folders;
		}

		public ImapTargetMailbox(ItemValue<Server> srv, TargetMailbox.Auth auth, int folders) {
			super(auth);
			this.sc = new StoreClient(srv.value.address(), 1143, auth.email(), auth.sid());
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
					Country country = new Faker().country();
					Beer beer = new Faker().beer();
					int lvl1 = folders;
					int lvl2 = folders;
					for (int i = 0; i < lvl1; i++) {
						String suffix = (Strings.padStart(Integer.toString(i), 3, '0') + " " + country.name())
								.replace(' ', '_');
						String root = auth.box().type == Mailbox.Type.user //
								? suffix //
								: "Dossiers partagés/" + auth.box().name + "/" + suffix;
						boolean created = sc.create(root);
						if (created) {
							target.add(root);
							for (int j = 0; j < lvl2; j++) {
								String sub = root + "/"
										+ (Strings.padStart(Integer.toString(j), 3, '0') + " " + beer.name())
												.replace(' ', '_');
								if (sc.create(sub)) {
									target.add(sub);
								} else {
									System.out.println("Not created: " + sub);
								}

							}
						} else {
							System.out.println("Not created: " + root);
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

		@Override
		public void exchange(TargetMailbox from, byte[] emlContent, long cycle) {
			try {
				lock.acquire();
			} catch (InterruptedException e1) {
				Thread.currentThread().interrupt();
				return;
			}
			try (InputStream in = new ByteArrayInputStream(emlContent)) {
				int added = sc.append(target.get(ThreadLocalRandom.current().nextInt(bound)), in, new FlagsList());
				logger.debug("Added {} to {}", added, auth.email());
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			} finally {
				lock.release();
			}
		}
	}

	public ImapInjector(IServiceProvider provider, String domainUid, IMessageProducer prod, DirEntryFilter filter,
			int folders) {
		super(provider, domainUid, auth -> new ImapTargetMailbox(auth, folders), prod, filter);
	}

	public ImapInjector(ItemValue<Server> srv, IServiceProvider provider, String domainUid) {
		this(srv, provider, domainUid, new GOTMessageProducer(), 5);
	}

	public ImapInjector(ItemValue<Server> srv, IServiceProvider provider, String domainUid, IMessageProducer prod,
			int folders) {
		super(provider, domainUid, auth -> new ImapTargetMailbox(srv, auth, folders), prod);
	}

}
