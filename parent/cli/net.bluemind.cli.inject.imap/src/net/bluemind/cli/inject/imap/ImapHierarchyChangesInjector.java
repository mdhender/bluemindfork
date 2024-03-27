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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

import net.bluemind.cli.inject.common.IMessageProducer;
import net.bluemind.cli.inject.common.MailExchangeInjector;
import net.bluemind.cli.inject.common.TargetMailbox;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.imap.ListInfo;
import net.bluemind.imap.StoreClient;
import net.bluemind.imap.TaggedResult;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.TagDescriptor;
import net.datafaker.Faker;
import net.datafaker.providers.food.Beer;

public class ImapHierarchyChangesInjector extends MailExchangeInjector {

	private static final AtomicLong START = new AtomicLong(System.currentTimeMillis());

	public static class ImapTargetMailbox extends TargetMailbox {
		StoreClient sc;
		Semaphore lock;
		private int folders;
		private final List<String> target;
		private final Beer faker;

		public ImapTargetMailbox(TargetMailbox.Auth auth, int folders) {
			super(auth);
			this.sc = new StoreClient(Topology.get().any(TagDescriptor.mail_imap.getTag()).value.address(), 1143,
					auth.email(), auth.sid());
			this.lock = new Semaphore(1);
			this.faker = new Faker().beer();
			this.target = new ArrayList<>(folders);
			this.folders = folders;
		}

		public boolean prepare() {
			boolean ret = sc.login();
			if (ret && folders > 0) {
				try {
					int i = 0;
					while (target.size() < folders) {
						String root = (Strings.padStart(Integer.toString(i), 3, '0') + " " + faker.name()).replace(' ',
								'_') + "_" + START.incrementAndGet();
						boolean created = sc.create(root);
						if (created) {
							target.add(root);
							i++;
						}
					}
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					ret = false;
				}
			}
			sc.listAll().stream()
					.filter(li -> li.isSelectable() && CharMatcher.inRange('0', '9').matches(li.getName().charAt(0)))
					.map(ListInfo::getName).forEach(target::add);
			folders = target.size();

			return ret;
		}

		public void exchange(TargetMailbox from, byte[] emlContent, long cycle) {
			try {
				lock.acquire();
			} catch (InterruptedException e1) {
				Thread.currentThread().interrupt();
				return;
			}
			try {
				if (cycle % 10 == 0) {
					String folderToChange = target.get(ThreadLocalRandom.current().nextInt(folders));
					boolean renamed = sc.rename(folderToChange, "Trash/" + folderToChange);
					if (!renamed) {
						throw new ServerFault("Failed to rename " + folderToChange);
					}
					boolean movedBack = sc.rename("Trash/" + folderToChange, folderToChange);
					if (!movedBack) {
						throw new ServerFault("Failed to move back " + folderToChange);
					}
				} else {
					TaggedResult fullList = sc.tagged("XLIST \"\" \"*\"");
					if (!fullList.isOk()) {
						throw new ServerFault("listing failed for " + auth.email());
					}
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			} finally {
				lock.release();
			}
		}
	}

	private static final IMessageProducer DUMB = new IMessageProducer() {
		private final byte[] empty = new byte[0];

		@Override
		public byte[] createEml(TargetMailbox from, TargetMailbox to) {
			return empty;
		}
	};

	public ImapHierarchyChangesInjector(IServiceProvider provider, String domainUid, int folders) {
		super(provider, domainUid, auth -> new ImapTargetMailbox(auth, folders), DUMB);
	}

	public ImapHierarchyChangesInjector(IServiceProvider provider, String domainUid) {
		this(provider, domainUid, 5);
	}

}
