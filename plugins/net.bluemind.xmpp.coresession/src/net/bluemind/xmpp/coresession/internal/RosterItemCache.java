/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.xmpp.coresession.internal;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class RosterItemCache {
	private static final Logger logger = LoggerFactory.getLogger(RosterItemCache.class);

	private static final Cache<String, RosterItem> items = Caffeine.newBuilder().recordStats()
			.expireAfterAccess(1, TimeUnit.HOURS).build();

	public static class CacheRegistration implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register(RosterItemCache.class, items);
		}
	}

	public static RosterItem get(String jabberId) {
		RosterItem ret = items.getIfPresent(jabberId);

		if (ret != null) {
			logger.debug("fetch {} from cache", jabberId);
			return ret;
		}

		logger.debug("fetch {} from DB", jabberId);

		try {
			String latd[] = jabberId.split("@");
			if (latd.length != 2) {
				logger.warn("try to fetch jabberId {} but it's not valid one", jabberId);
				return null;
			}
			IDomains domService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IDomains.class);
			ItemValue<Domain> domain = domService.findByNameOrAliases(latd[1]);

			if (domain == null) {
				logger.warn("Cannot find domain {}", latd[1]);
				return null;
			}

			IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
					domain.uid);
			ItemValue<User> user = userService.byEmail(jabberId);
			if (user != null) {
				ret = new RosterItem();
				ret.user = user;
				byte[] photo = userService.getPhoto(user.uid);
				if (photo != null) {
					ret.photo = Base64.getEncoder().encodeToString(photo);
				}
				items.put(jabberId, ret);
			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return ret;
	}

	public static void invalidate(BmContext context, User user) {
		logger.debug("invalidate {} from cache", user.defaultEmail());
		items.invalidate(user.login + "@" + context.getSecurityContext().getContainerUid());
		if (user.defaultEmail() != null) {
			items.invalidate(user.defaultEmail());
		}
	}

}
