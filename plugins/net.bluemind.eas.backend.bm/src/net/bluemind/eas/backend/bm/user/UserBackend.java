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
package net.bluemind.eas.backend.bm.user;

import java.util.Base64;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Splitter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.bluemind.config.Token;
import net.bluemind.core.api.Email;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.document.storage.IDocumentStore;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.bm.impl.CoreConnect;
import net.bluemind.eas.dto.user.MSUser;
import net.bluemind.eas.exception.ActiveSyncException;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.network.topology.Topology;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserSettings;
import net.bluemind.user.api.User;

public class UserBackend extends CoreConnect {
	private static final Cache<String, MSUser> cache = CacheBuilder.newBuilder()
			.recordStats()
			.expireAfterAccess(1, TimeUnit.MINUTES)
			.build();

	public static class CacheRegistration implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register(UserBackend.class, cache);
		}
	}

	public static void purgeSession() {
		cache.invalidateAll();
	}

	public MSUser getUser(String loginAtDomain, String password) throws ActiveSyncException {
		MSUser ret = cache.getIfPresent(loginAtDomain);
		if (ret == null) {
			ret = getUserImpl(loginAtDomain, password);
			cache.put(loginAtDomain, ret);
		} else {
			logger.debug("[{}] using cached user.", loginAtDomain);
		}
		return ret;
	}

	private MSUser getUserImpl(String loginAtDomain, String password) throws ActiveSyncException {
		Iterator<String> latd = Splitter.on("@").split(loginAtDomain).iterator();
		@SuppressWarnings("unused")
		String login = latd.next();
		String domain = latd.next();
		try {
			String core = "http://" + Topology.get().core().value.address() + ":8090";

			// BM-8155
			IDomains domainsService = getService(core, Token.admin0(), IDomains.class);
			ItemValue<Domain> dom = domainsService.findByNameOrAliases(domain);

			ItemValue<User> user = getService(core, Token.admin0(), IUser.class, dom.uid).byEmail(loginAtDomain);
			Map<String, String> settings = getService(core, Token.admin0(), IUserSettings.class, dom.uid).get(user.uid);
			String lang = settings.get("lang");
			String tz = settings.get("timezone");
			Set<String> emails = new HashSet<String>();
			String defaultEmail = user.value.defaultEmail().address;
			for (Email e : user.value.emails) {
				if (e.allAliases) {
					for (String alias : dom.value.aliases) {
						String email = e.address.split("@")[0] + "@" + alias;
						if (!defaultEmail.equals(email)) {
							emails.add(email);
						}
					}
				}
				emails.add(e.address);
			}
			MSUser ret = new MSUser(user.uid, user.displayName, user.value.login + "@" + dom.uid, password, lang, tz,
					user.value.routing != Routing.none, user.value.defaultEmail().address, emails,
					user.value.dataLocation);
			return ret;
		} catch (Exception e) {
			throw new ActiveSyncException(e);
		}
	}

	private String loadPhoto(Integer photoId, BackendSession bs) {
		// may not work, but you get the idea
		try {
			byte[] b = getService(bs, IDocumentStore.class).get(Integer.toString(photoId));
			return Base64.getEncoder().encodeToString(b);
		} catch (Exception e) {
			logger.error("Fail to fetch photo {}", photoId);
		}
		return null;
	}

	public String getPictureBase64(BackendSession bs, int photoId) {
		return loadPhoto(photoId, bs);
	}

}
