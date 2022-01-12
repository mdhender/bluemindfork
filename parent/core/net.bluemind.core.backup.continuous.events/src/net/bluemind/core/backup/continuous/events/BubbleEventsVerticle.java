/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.core.backup.continuous.events;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.RateLimiter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.calendar.hook.CalendarHookAddress;
import net.bluemind.config.InstallationId;
import net.bluemind.core.backup.continuous.DefaultBackupStore;
import net.bluemind.core.backup.continuous.api.IBackupStore;
import net.bluemind.core.backup.continuous.api.IBackupStoreFactory;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.user.api.User;
import net.bluemind.user.service.IInCoreUser;

public class BubbleEventsVerticle extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(BubbleEventsVerticle.class);

	private static final String ADDR = "mailreplica.mailbox.updated";

	public static final String BUBBLE_ADDR = "bubble.owner";

	public static class Factory implements IVerticleFactory, IUniqueVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new BubbleEventsVerticle();
		}

	}

	public static class FullDirEntry<T> {

		public final DirEntry entry;

		public final VCard vcard;

		public final Mailbox mailbox;

		public final T value;

		public FullDirEntry(DirEntry de, VCard vcard, Mailbox mailbox, T value) {
			this.entry = de;
			this.vcard = vcard;
			this.mailbox = mailbox;
			this.value = value;
		}

	}

	public static final Map<String, RateLimiter> ownerRateLimit = new ConcurrentHashMap<>();
	public static final Map<String, RateLimiter> domainRateLimit = new ConcurrentHashMap<>();

	@Override
	public void start(Promise<Void> startPromise) throws Exception {

		Handler<Message<JsonObject>> handler = msg -> {
			JsonObject js = msg.body();
			String domain = js.getString("domain");
			String owner = js.getString("owner");

			if (owner.equals(domain)) {
				bubbleDomain(domain);
			} else {
				bubbleOwner(domain, owner);
			}
		};

		vertx.eventBus().localConsumer(ADDR, handler);
		vertx.eventBus().localConsumer(BUBBLE_ADDR, handler);

		vertx.eventBus().localConsumer(CalendarHookAddress.CHANGED, (Message<JsonObject> msg) -> {
			JsonObject js = msg.body();
			String domain = js.getString("domainUid");
			// the field seems badly named in CalendarEventProducer
			String owner = js.getString("loginAtDomain");
			bubbleOwner(domain, owner);
		});

		vertx.eventBus().localConsumer("dir.changed", (Message<JsonObject> msg) -> {
			String dom = msg.body().getString("domain");
			bubbleDomain(dom);
		});

		startPromise.complete();
	}

	private void bubbleOwner(String domain, String owner) {
		String ownerKey = owner + "@" + domain;
		RateLimiter rateLimit = ownerRateLimit.computeIfAbsent(ownerKey, k -> {
			return RateLimiter.create(0.2);
		});

		if (rateLimit.tryAcquire()) {
			logger.info("Bubbling for d: {}, owner: {}", domain, owner);
			IBackupStoreFactory backup = DefaultBackupStore.store();
			ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
			IDirectory dirApi = prov.instance(IDirectory.class, domain);
			DirEntry de = dirApi.findByEntryUid(owner);

			if (de == null) {
				return;
			}

			IMailboxes mboxApi = prov.instance(IMailboxes.class, domain);
			ItemValue<Mailbox> mbox = mboxApi.getComplete(owner);
			BaseContainerDescriptor dir = new BaseContainerDescriptor();
			dir.defaultContainer = true;
			dir.uid = domain;
			dir.owner = "system";
			dir.domainUid = domain;
			dir.type = "dir";
			dir.name = domain;

			switch (de.kind) {
			case USER:
				IInCoreUser userApi = prov.instance(IInCoreUser.class, domain);
				ItemValue<User> user = userApi.getFull(owner);
				FullDirEntry<User> fde = new FullDirEntry<>(de, user.value.contactInfos, mbox.value, user.value);
				ItemValue<FullDirEntry<User>> iv = ItemValue.create(user, fde);
				IBackupStore<FullDirEntry<User>> store = backup.<FullDirEntry<User>>forContainer(dir);
				store.store(iv);
				logger.info("Mark entry {} as updated.", ownerKey);
				break;
			case MAILSHARE:
				break;
			default:
				logger.warn("not managed yet");
				break;
			}

		}

		bubbleDomain(domain);
	}

	private void bubbleDomain(String domain) {
		RateLimiter domLimit = domainRateLimit.computeIfAbsent(domain, k -> {
			return RateLimiter.create(0.2);
		});

		if (domLimit.tryAcquire()) {
			IBackupStoreFactory backup = DefaultBackupStore.store();
			ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

			BaseContainerDescriptor domains = new BaseContainerDescriptor();
			domains.defaultContainer = true;
			domains.uid = "domains_" + InstallationId.getIdentifier();
			domains.owner = "system";
			domains.type = "domains";
			domains.name = "domains";

			IDomains domApi = prov.instance(IDomains.class);
			ItemValue<Domain> domItem = domApi.get(domain);
			IBackupStore<Domain> store = backup.forContainer(domains);
			store.store(domItem);
			logger.info("Mark {} as updated.", domain);
		}
	}

}
