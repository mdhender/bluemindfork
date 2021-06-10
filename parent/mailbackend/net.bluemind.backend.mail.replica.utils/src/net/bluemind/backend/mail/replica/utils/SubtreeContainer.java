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

import com.github.benmanes.caffeine.cache.Caffeine;

import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.backend.mail.replica.api.utils.Subtree;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.caches.registry.CacheHolder;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.external.ExternalDirectories;
import net.bluemind.directory.external.IExternalDirectory;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;

public class SubtreeContainer {

	private SubtreeContainer() {
	}

	private static final Logger logger = LoggerFactory.getLogger(SubtreeContainer.class);

	public static Subtree mailSubtreeUid(BmContext ctx, String domainUid, MailboxReplicaRootDescriptor mr) {
		logger.debug("Compute subtree uid for {} @ {}", mr, domainUid);
		String ownerUid = owner(ctx, mr, domainUid);
		return mailSubtreeUid(domainUid, mr.ns, ownerUid);
	}

	public static Subtree mailSubtreeUid(String domainUid, Namespace ns, String ownerUid) {
		Subtree ret = new Subtree();
		ret.domainUid = domainUid;
		ret.namespace = ns;
		ret.ownerUid = ownerUid;
		return ret;

	}

	public static class Registration implements ICacheRegistration {

		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register("subtreeContainerMboxes",
					Caffeine.newBuilder().recordStats().expireAfterWrite(2, TimeUnit.MINUTES).build());
		}
	}

	private static String owner(BmContext context, MailboxReplicaRootDescriptor root, String domainOrPartition) {
		String domainUid = domainOrPartition.replace('_', '.');
		String nameOrUid = root.name.replace('^', '.');
		CacheRegistry cacheRegistry = context.provider().instance(CacheRegistry.class);
		CacheHolder<String, String> cache;
		if (cacheRegistry != null) {
			cache = CacheHolder.of(cacheRegistry.get("subtreeContainerMboxes"));
		} else {
			// backup context does not uses caches
			cache = CacheHolder.of(null);
		}
		String cacheKey = nameOrUid + "@" + domainUid;
		return Optional.ofNullable(cache.getIfPresent(cacheKey)).orElseGet(() -> {
			IMailboxes mboxApi = context.su().provider().instance(IMailboxes.class, domainUid);
			ItemValue<Mailbox> mboxIv = mboxApi.byName(nameOrUid);
			if (mboxIv == null) {
				mboxIv = mboxApi.getComplete(nameOrUid);
			}
			if (mboxIv == null) {
				ExternalDirectories dirs = new ExternalDirectories(domainUid);
				for (IExternalDirectory ed : dirs.dirs()) {
					mboxIv = ed.findByName(nameOrUid);
					if (mboxIv != null) {
						break;
					}
				}
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
