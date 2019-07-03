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
package net.bluemind.milter.action.signature;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.json.JsonObject;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.mailflow.rbe.IClientContext;

public class DirectoryCache extends BusModBase {

	private static Map<String, VCard> uidToVCard = new ConcurrentHashMap<>();
	private static Map<String, String> emailToUid = new ConcurrentHashMap<>();
	private static Cache<String, String> noVCards = CacheBuilder.newBuilder().expireAfterWrite(20, TimeUnit.MINUTES)
			.build();

	private static final Logger logger = LoggerFactory.getLogger(DirectoryCache.class);

	@Override
	public void start() {
		super.start();
		logger.info("Registering directory cache listener");
		super.eb.registerHandler(MilterMessageForwarder.eventAddressChanged, (message) -> {
			JsonObject jsonObject = (JsonObject) message.body();
			String domainUid = jsonObject.getString("domain");
			String uid = jsonObject.getString("uid");
			logger.info("Invalidating directory cache for {}@{}", uid, domainUid);
			uidToVCard.remove(domainUid + "#" + uid);
		});
	}

	public static Optional<VCard> getVCard(IClientContext mailflowContext, String domain, String email) {

		if (noVCards.getIfPresent(email) != null) {
			return Optional.empty();
		}

		VCard card = null;
		String uid = emailToUid.get(email);
		if (uid != null) {
			card = uidToVCard.get(uid);
		}

		if (card == null) {
			if (mailflowContext != null && domain != null) {
				Optional<ItemValue<VCard>> resolved = resolveVCard(mailflowContext, email, domain);
				if (!resolved.isPresent()) {
					noVCards.put(email, email);
					return Optional.empty();
				} else {
					card = resolved.get().value;
					emailToUid.put(email, domain + "#" + resolved.get().uid);
					uidToVCard.put(domain + "#" + resolved.get().uid, resolved.get().value);
				}
			} else {
				return Optional.empty();
			}
		}
		return Optional.of(card);
	}

	private static Optional<ItemValue<VCard>> resolveVCard(IClientContext context, String sender, String domain) {
		try {
			IDirectory dir = context.provider().instance(IDirectory.class, domain);
			DirEntry result = dir.getByEmail(sender);
			if (result != null) {
				return Optional.ofNullable(dir.getVCard(result.entryUid));
			}
		} catch (ServerFault e) {
			logger.warn("Cannot find vcard of {}", sender, e);	
		}
		return Optional.empty();
	}

}
