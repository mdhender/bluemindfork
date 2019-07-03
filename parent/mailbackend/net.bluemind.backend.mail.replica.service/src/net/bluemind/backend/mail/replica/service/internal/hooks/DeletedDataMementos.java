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

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.backend.mail.replica.utils.SubtreeContainer;
import net.bluemind.backend.mail.replica.utils.SubtreeContainer.Subtree;
import net.bluemind.core.caches.registry.CacheHolder;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.mailbox.api.Mailbox;

public class DeletedDataMementos extends CacheHolder<String, Subtree> {

	public static final Logger logger = LoggerFactory.getLogger(DeletedDataMementos.class);

	private static Cache<String, Subtree> buildCache() {
		return CacheBuilder.newBuilder().expireAfterWrite(2, TimeUnit.MINUTES).build();
	}

	public static class Registration implements ICacheRegistration {

		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register("deletedDataMementos", buildCache());
		}
	}

	public DeletedDataMementos(Cache<String, Subtree> cache) {
		super(cache);
	}

	public static DeletedDataMementos get(BmContext context) {
		if (context == null || context.provider().instance(CacheRegistry.class) == null) {
			return new DeletedDataMementos(null);
		} else {
			return new DeletedDataMementos(context.provider().instance(CacheRegistry.class).get("deletedDataMementos"));
		}
	}

	public static void preDelete(BmContext ctx, String domainUid, ItemValue<Mailbox> mbox) {
		if (mbox.value.dataLocation == null) {
			logger.warn("mbox without datalocation {}", mbox);
			return;
		}
		DeletedDataMementos mementos = DeletedDataMementos.get(ctx);
		logger.info("Remembering with {}", mementos);

		String root = domainUid + "!" + mbox.value.type.nsPrefix + mbox.value.name.replace('.', '^');
		Subtree st = SubtreeContainer.mailSubtreeUid(domainUid,
				mbox.value.type.sharedNs ? Namespace.shared : Namespace.users, mbox.uid);
		logger.info("Caching {} => {} for future use.", root, st.subtreeUid);
		mementos.put(root, st);
	}

	public static Subtree cachedSubtree(BmContext context, String domainUid, MailboxReplicaRootDescriptor mailboxRoot) {
		DeletedDataMementos ctxCache = DeletedDataMementos.get(context);
		String key = domainUid + "!" + mailboxRoot.ns.prefix() + mailboxRoot.name;
		logger.debug("Looking for {}", key);
		return ctxCache.getIfPresent(key);
	}
}
