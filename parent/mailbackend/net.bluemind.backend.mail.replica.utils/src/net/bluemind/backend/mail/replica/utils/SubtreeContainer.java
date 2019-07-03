/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.backend.mail.replica.utils;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.cache.CacheBuilder;

import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.caches.registry.CacheHolder;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;

public class SubtreeContainer {

	private static final Logger logger = LoggerFactory.getLogger(SubtreeContainer.class);

	public static class Subtree {
		public Subtree(String sub, String owner) {
			this.subtreeUid = sub;
			this.ownerUid = owner;
		}

		public final String subtreeUid;
		public final String ownerUid;

		public String toString() {
			return MoreObjects.toStringHelper(Subtree.class)//
					.add("subtree", subtreeUid)//
					.add("owner", ownerUid)//
					.toString();
		}
	}

	public static Subtree mailSubtreeUid(BmContext ctx, String domainUid, MailboxReplicaRootDescriptor mr) {
		logger.debug("Compute subtree uid for {} @ {}", mr, domainUid);
		String ownerUid = owner(ctx, mr, domainUid);
		return mailSubtreeUid(domainUid, mr.ns, ownerUid);
	}

	public static Subtree mailSubtreeUid(String domainUid, Namespace ns, String ownerUid) {
		String sub = "subtree_" + domainUid.replace('.', '_') + "!" + ns.prefix() + ownerUid;
		return new Subtree(sub, ownerUid);
	}

	public static class Registration implements ICacheRegistration {

		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register("subtreeContainerMboxes",
					CacheBuilder.newBuilder().expireAfterWrite(2, TimeUnit.MINUTES).build());
		}
	}

	private static String owner(BmContext context, MailboxReplicaRootDescriptor root, String domainOrPartition) {
		String domainUid = domainOrPartition.replace('_', '.');
		String nameOrUid = root.name.replace('^', '.');
		CacheHolder<String, String> cache = CacheHolder
				.of(context.provider().instance(CacheRegistry.class).get("subtreeContainerMboxes"));
		String cacheKey = nameOrUid + "@" + domainUid;
		return Optional.ofNullable(cache.getIfPresent(cacheKey)).orElseGet(() -> {
			IMailboxes mboxApi = context.su().provider().instance(IMailboxes.class, domainUid);
			ItemValue<Mailbox> mboxIv = mboxApi.byName(nameOrUid);
			if (mboxIv == null) {
				mboxIv = mboxApi.getComplete(nameOrUid);
			}
			if (mboxIv == null) {
				throw new ServerFault(
						"Owner " + nameOrUid + "@" + domainUid + "(" + domainOrPartition + ") does not exist");
			} else {
				return cacheAndReturn(cache, cacheKey, mboxIv);
			}
		});
	}

	private static String cacheAndReturn(CacheHolder<String, String> cache, String cacheKey,
			ItemValue<Mailbox> mboxIv) {
		cache.put(cacheKey, mboxIv.uid);
		return mboxIv.uid;
	}
}
