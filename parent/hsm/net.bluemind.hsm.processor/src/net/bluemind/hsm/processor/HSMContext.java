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
package net.bluemind.hsm.processor;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.hsm.storage.HSMStorage;
import net.bluemind.hsm.storage.IHSMStorage;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.StoreClient;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.user.api.IUserSettings;

public class HSMContext {
	private SecurityContext context;
	private HSMLoginContext loginContext;
	private IHSMStorage storage;
	private String lang;

	private static final Cache<String, HSMContext> hsmCtxCache = Caffeine.newBuilder().recordStats().initialCapacity(64)
			.expireAfterAccess(2, TimeUnit.MINUTES).build();

	public static class CacheRegistration implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register(HSMContext.class, hsmCtxCache);
		}
	}

	public static HSMContext get(SecurityContext context, HSMLoginContext login) throws ServerFault {

		HSMContext ctx = hsmCtxCache.getIfPresent(context.getSessionId());
		if (ctx == null) {
			ctx = new HSMContext(context, login);
		}

		return ctx;

	}

	private HSMContext(SecurityContext context, HSMLoginContext login) throws ServerFault {
		this.context = context;
		this.loginContext = login;

		Map<String, String> settings = ServerSideServiceProvider.getProvider(context)
				.instance(IUserSettings.class, context.getContainerUid()).get(context.getSubject());

		if (settings.containsKey("lang")) {
			lang = settings.get("lang");
		} else {
			lang = "en";
		}

		// we have an NFS mount on the data backend & we rely on that for the archive
		// partition.
		INodeClient nc = NodeActivator.get(loginContext.dataLocation);

		storage = HSMStorage.storage;
		storage.open(nc);

	}

	public StoreClient connect(String folder) throws IMAPException {
		StoreClient ret = new StoreClient(loginContext.dataLocation, 1143,
				loginContext.login + "@" + context.getContainerUid(), context.getSessionId());

		if (!ret.login()) {
			throw new IMAPException("Fail to login");
		}

		if (!ret.select(folder)) {
			throw new IMAPException("Fail to select folder " + folder);
		}

		return ret;
	}

	public IHSMStorage getHSMStorage() {
		return storage;
	}

	public HSMLoginContext getLoginContext() {
		return loginContext;
	}

	public String getLang() {
		return lang;
	}

	public SecurityContext getSecurityContext() {
		return context;
	}

	public static class HSMLoginContext {
		public final String login;
		public final String uid;
		/**
		 * this holds the server address, not its uid
		 */
		public final String dataLocation;

		public HSMLoginContext(String login, String uid, String dataLocation) {
			this.login = login;
			this.uid = uid;
			this.dataLocation = dataLocation;
		}
	}
}
