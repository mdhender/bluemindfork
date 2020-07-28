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
package net.bluemind.xmpp.server;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.ValidationKind;
import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.im.api.IInstantMessaging;
import net.bluemind.network.topology.Topology;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserSettings;
import net.bluemind.user.api.User;
import tigase.db.TigaseDBException;
import tigase.xmpp.BareJID;

public final class CF {
	private static final Logger logger = LoggerFactory.getLogger(CF.class);
	private static final Cache<BareJID, String> idIndex = CacheBuilder.newBuilder()
			.recordStats()
			.expireAfterAccess(20, TimeUnit.MINUTES)
			.initialCapacity(1024)
			.build();
	private static final Cache<String, ItemValue<Domain>> domainCache = CacheBuilder.newBuilder()
			.recordStats()
			.expireAfterAccess(1, TimeUnit.HOURS)
			.initialCapacity(1024)
			.build();
	private static String coreIp;

	public static class CacheRegistration implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register("xmpp-server-cf-idindex", idIndex);
			cr.register("xmpp-server-cf-domain", domainCache);
		}
	}

	/**
	 * @param l
	 * @param p
	 * @return
	 */
	public final static boolean login(String login, String password) {
		boolean ret = false;
		try {
			ItemValue<User> user = user(BareJID.bareJIDInstance(login));
			if (user == null) {
				return false;
			}

			IAuthentication authService = provider().instance(IAuthentication.class);
			ValidationKind resp = authService.validate(login, password, "bm-xmpp");
			if (resp == ValidationKind.NONE || resp == ValidationKind.PASSWORDEXPIRED) {
				return false;
			}

			// TODO RPC contains instantmessaging/canAccess.
			ret = true;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return ret;
	}

	/**
	 * @param jid
	 * @return
	 * @throws TigaseDBException
	 */
	public static ItemValue<User> user(BareJID jid) throws TigaseDBException {
		try {
			ItemValue<Domain> domain = getDomain(jid.getDomain());
			IUser service = provider().instance(IUser.class, domain.uid);
			ItemValue<User> user = service.byEmail(jid.toString());
			if (user != null) {
				return user;
			} else {
				logger.error("Cannot find user {}", jid.toString());
				throw new TigaseDBException("Cannot find user " + jid.toString());
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new TigaseDBException(e.getMessage(), e);
		}
	}

	/**
	 * @param jid
	 * @return
	 * @throws TigaseDBException
	 */
	public static byte[] userPhoto(BareJID jid) throws TigaseDBException {
		String uid = jidToUid(jid);

		if (uid != null) {
			ItemValue<Domain> domain = getDomain(jid.getDomain());
			IUser service = provider().instance(IUser.class, domain.uid);
			return service.getPhoto(uid);
		}

		logger.error("Cannot find user {}", jid.toString());
		throw new TigaseDBException("Cannot find user " + jid.toString());

	}

	/**
	 * @param u
	 * @return
	 * @throws ServerFault
	 * @throws TigaseDBException
	 */
	public static String getLang(ItemValue<User> u, BareJID jid) throws ServerFault, TigaseDBException {
		Map<String, String> settings = provider().instance(IUserSettings.class, getDomain(jid.getDomain()).uid)
				.get(u.uid);
		return settings.get("lang");
	}

	/**
	 * @param jid
	 * @return
	 * @throws TigaseDBException
	 */
	public static String jidToUid(BareJID jid) throws TigaseDBException {
		String uid = idIndex.getIfPresent(jid);
		if (uid != null) {
			return uid;
		}
		try {
			ItemValue<User> u = user(jid);
			if (u != null) {
				uid = u.value.defaultEmail().address;
				idIndex.put(jid, uid);
			}
		} catch (Exception e) {
			throw new TigaseDBException(e.getMessage(), e);
		}
		return uid;
	}

	public static ItemValue<Domain> getDomain(String name) throws TigaseDBException {
		ItemValue<Domain> dom = domainCache.getIfPresent(name);
		if (dom != null) {
			return dom;
		}
		try {
			IDomains domService = provider().instance(IDomains.class);
			dom = domService.findByNameOrAliases(name);
			if (dom != null) {
				domainCache.put(name, dom);
			} else {
				logger.error("Cannot find domain {}", name);
				throw new TigaseDBException("Cannot find domain " + name);
			}
		} catch (Exception e) {
			throw new TigaseDBException(e.getMessage(), e);
		}
		return dom;
	}

	/**
	 * @param jabberId
	 * @param node
	 * @return
	 * @throws TigaseDBException
	 */
	public static String getRoster(String jabberId) throws TigaseDBException {
		String ret = null;
		try {
			IInstantMessaging service = provider().instance(IInstantMessaging.class);
			String uid = jidToUid(BareJID.bareJIDInstance(jabberId));

			logger.debug("get roster for user {}", uid);

			ret = service.getRoster(uid);

		} catch (Exception e) {
			throw new TigaseDBException(e.getMessage(), e);
		}
		return ret;
	}

	/**
	 * @param string
	 * @param node
	 * @param value
	 * @throws TigaseDBException
	 */
	public static void setRoster(String jabberId, String data) throws TigaseDBException {
		try {
			IInstantMessaging service = provider().instance(IInstantMessaging.class);
			String uid = jidToUid(BareJID.bareJIDInstance(jabberId));

			logger.debug("set roster for user {} : {}", uid, data);

			service.setRoster(uid, data);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new TigaseDBException(e.getMessage(), e);
		}
	}

	public static IServiceProvider provider() {
		String uri = "http://" + locate() + ":8090";
		return ClientSideServiceProvider.getProvider(uri, Token.admin0()).setOrigin("xmpp");
	}

	protected static String locate() {
		return Topology.getIfAvailable().map(t -> t.core().value.address()).orElse("127.0.0.1");
	}

}
