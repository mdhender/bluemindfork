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
package net.bluemind.backend.mail.replica.service.internal.hooks;

import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.backend.mail.replica.api.utils.Subtree;
import net.bluemind.backend.mail.replica.persistence.DeletedMailboxesStore;
import net.bluemind.core.caches.registry.CacheHolder;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.mailbox.api.Mailbox;

public class DeletedDataMementos extends CacheHolder<String, Optional<Subtree>> {
	public static final Logger logger = LoggerFactory.getLogger(DeletedDataMementos.class);

	public static class Registration implements ICacheRegistration {

		private static Cache<String, Optional<Subtree>> buildCache() {
			return CacheBuilder.newBuilder().recordStats().expireAfterWrite(2, TimeUnit.MINUTES).build();
		}

		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register("deletedDataMementos", buildCache());
		}

	}

	public DeletedDataMementos(Cache<String, Optional<Subtree>> cache) {
		super(cache);
	}

	public static DeletedDataMementos get(BmContext context) {
		if (context == null || context.provider().instance(CacheRegistry.class) == null) {
			return new DeletedDataMementos(null);
		}
		return new DeletedDataMementos(context.provider().instance(CacheRegistry.class).get("deletedDataMementos"));
	}

	public static void forgetDeletion(BmContext ctx, String domainUid, Mailbox box) {
		DeletedDataMementos mementos = DeletedDataMementos.get(ctx);
		String key = cacheKey(domainUid, box);
		mementos.invalidate(key);
	}

	public static void preDelete(BmContext ctx, String domainUid, ItemValue<Mailbox> mbox) {
		if (mbox.value.dataLocation == null) {
			logger.warn("mbox without datalocation {}", mbox);
			return;
		}
		DeletedDataMementos mementos = DeletedDataMementos.get(ctx);
		logger.info("Remembering with {}", mementos);

		DeletedMailboxesStore store = new DeletedMailboxesStore(ctx.getDataSource());

		Subtree subtree = new Subtree();
		subtree.ownerUid = mbox.uid;
		subtree.mailboxName = mbox.value.name;
		subtree.namespace = mbox.value.type.sharedNs ? Namespace.shared : Namespace.users;
		subtree.domainUid = domainUid;

		try {
			store.store(subtree);
			mementos.put(subtree.subtreeUid(), Optional.of(subtree));
		} catch (SQLException e) {
			logger.warn(e.getMessage(), e);
		}

	}

	private static String cacheKey(String domainUid, MailboxReplicaRootDescriptor mailboxRoot) {
		return domainUid + "!" + mailboxRoot.ns.prefix() + mailboxRoot.name;
	}

	private static String cacheKey(String domainUid, Mailbox mbox) {
		return domainUid + "!" + mbox.type.nsPrefix + mbox.name;
	}

	public static Subtree cachedSubtree(BmContext context, String domainUid, MailboxReplicaRootDescriptor mailboxRoot) {
		DeletedDataMementos ctxCache = DeletedDataMementos.get(context);
		String key = cacheKey(domainUid, mailboxRoot);
		logger.debug("Looking for {}", key);
		Optional<Subtree> subtree = ctxCache.getIfPresent(key);

		if (subtree == null) {
			DeletedMailboxesStore store = new DeletedMailboxesStore(context.getDataSource());
			try {
				Subtree fromDb = store.getByMboxName(domainUid, mailboxRoot.name);
				subtree = Optional.ofNullable(fromDb);
				ctxCache.put(key, subtree);
			} catch (SQLException e) {
				logger.warn("Failed to get Subtree for {} {}: {}", domainUid, mailboxRoot.name, e.getMessage(), e);
			}
		}

		return subtree.orElse(null);

	}

}
