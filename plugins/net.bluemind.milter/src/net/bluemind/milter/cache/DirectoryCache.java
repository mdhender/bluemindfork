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
package net.bluemind.milter.cache;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.mailflow.rbe.IClientContext;
import net.bluemind.milter.mq.MilterMessageForwarder;
import net.bluemind.network.topology.Topology;

public class DirectoryCache extends AbstractVerticle {
	private static Optional<IServiceProvider> provider = Optional.empty();
	private static Map<String, Long> changesetVersion = new ConcurrentHashMap<>();
	private static Map<String, VCard> uidToVCard = new ConcurrentHashMap<>();
	protected static Map<String, String> emailToUid = new ConcurrentHashMap<>();
	private static Cache<String, String> noVCards = Caffeine.newBuilder().recordStats()
			.expireAfterWrite(20, TimeUnit.MINUTES).build();

	private static final Logger logger = LoggerFactory.getLogger(DirectoryCache.class);

	public static class CacheRegistration implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register(DirectoryCache.class, noVCards);
		}
	}

	@Override
	public void start() {

		logger.info("Registering directory cache listener");
		EventBus eb = vertx.eventBus();
		eb.consumer(MilterMessageForwarder.eventAddressChanged, (message) -> {
			if (!provider.isPresent()) {
				String host = "http://" + Topology.get().core().value.address() + ":8090";
				provider = Optional.ofNullable(ClientSideServiceProvider.getProvider(host, Token.admin0()));
				if (!provider.isPresent()) {
					logger.error("Not able to update the cache. Will retry it when receiving next dir.changed event");
					return;
				}
			}

			String domainUid = ((JsonObject) message.body()).getString("domain");

			IDomains domainService = provider.get().instance(IDomains.class);
			ItemValue<Domain> domainVal = domainService.get(domainUid);
			if (domainVal == null) {
				logger.warn("Not able to update the changeset because domain {} not exists", domainUid);
				return;
			}

			Long lastVersion = changesetVersion.getOrDefault(domainUid, Long.valueOf(0));

			ContainerChangeset<String> changeset = provider.get().instance(IDirectory.class, domainUid)
					.changeset(lastVersion);

			if (!uidToVCard.isEmpty()) {
				// checking if our uids are concerned by changeset
				Stream.concat(changeset.updated.stream(), changeset.deleted.stream())
						.filter(uid -> uidToVCard.containsKey(domainUid + "#" + uid)).forEach(uid -> {
							uidToVCard.remove(domainUid + "#" + uid);
							logger.info("Invalidating directory cache for {}@{}", uid, domainUid);
						});
			}

			changesetVersion.put(domainUid, changeset.version);
		});
	}

	public static Optional<VCard> getVCard(IClientContext clientContext, String domain, String email) {
		if (noVCards.getIfPresent(email) != null) {
			return Optional.empty();
		}

		VCard card = null;
		String uid = Optional.ofNullable(emailToUid.get(email))
				.orElseGet(() -> getUserUidByEmail(clientContext, domain, email).orElse(null));
		if (uid != null) {
			card = uidToVCard.get(domain + "#" + uid);
			if (card == null) {
				resolveCaches(clientContext, domain, email);
				card = uidToVCard.get(domain + "#" + uid);
			}
		}

		return Optional.ofNullable(card);

	}

	public static Optional<String> getUserUidByEmail(IClientContext clientContext, String domain, String email) {
		if (noVCards.getIfPresent(email) != null) {
			return Optional.empty();
		}

		String uid = emailToUid.get(email);
		if (uid == null) {
			resolveCaches(clientContext, domain, email);
			uid = emailToUid.get(email);
		}

		if (uid != null) {
			return Optional.ofNullable(uid.split(domain + "#")[1]);
		}
		return Optional.ofNullable(uid);
	}

	private static Optional<ItemValue<VCard>> resolveVCard(IClientContext clientContext, String email, String domain) {
		try {
			IDirectory dir = clientContext.provider().instance(IDirectory.class, domain);
			DirEntry result = dir.getByEmail(email);
			if (result != null) {
				return Optional.ofNullable(dir.getVCard(result.entryUid));
			}

		} catch (ServerFault e) {
			logger.warn("Cannot find vcard of {}", email, e);
		}
		return Optional.empty();
	}

	private static void resolveCaches(IClientContext clientContext, String domain, String email) {
		String uid = emailToUid.get(email);
		if (clientContext != null && domain != null && (uid == null || uidToVCard.get(uid) == null)) {
			resolveVCard(clientContext, email, domain).ifPresentOrElse(card -> {
				emailToUid.put(email, domain + "#" + card.uid);
				uidToVCard.put(domain + "#" + card.uid, card.value);
			}, () -> {
				noVCards.put(email, email);
			});
		}

	}

}
