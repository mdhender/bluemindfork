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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.lmtp.filter.imip.cache;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;

public class MailboxCache extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(MailboxCache.class);
	private static Cache<String, Optional<ItemValue<Mailbox>>> nameToMailbox;
	private static Map<String, String> uidToName = new ConcurrentHashMap<>();

	static {
		nameToMailbox = CacheBuilder.newBuilder().expireAfterAccess(30, TimeUnit.MINUTES).concurrencyLevel(4)
				.initialCapacity(1024) //
				.build();
	}

	@Override
	public void start() {
		logger.info("Registering mailbox cache listener");

		vertx.eventBus().consumer(MailboxMessageForwarder.mailboxChanged, message -> {
			JsonObject eventData = (JsonObject) message.body();
			String uid = key(eventData.getString("mailbox"), eventData.getString("domain"));
			logger.debug("Invalidating mailbox {}", uid);
			if (uidToName.containsKey(uid)) {
				nameToMailbox.invalidate(uidToName.get(uid));
				uidToName.remove(uid);
			}
		});
	}

	public static Optional<ItemValue<Mailbox>> get(IServiceProvider provider, String domain, String box) {
		try {
			return nameToMailbox.get(key(box, domain), (() -> {
				IMailboxes mailboxService = provider.instance(IMailboxes.class, domain);
				ItemValue<Mailbox> mailbox = mailboxService.byName(box);
				if (mailbox == null) {
					return Optional.empty();
				}
				uidToName.put(key(mailbox.uid, domain), key(box, domain));
				return Optional.of(mailbox);
			}));
		} catch (Exception e) {
			logger.warn("Cannot verify mailbox by name {}", box, e);
			return Optional.empty();
		}

	}

	private static String key(String uid, String domain) {
		return uid + "@" + domain;
	}
}
