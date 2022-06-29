/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.backend.mail.replica.service.internal;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;

import net.bluemind.authentication.api.AuthUser;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.imap.StoreClient;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.sysconf.helper.LocalSysconfCache;

public class ImapContext {
	private static final Logger logger = LoggerFactory.getLogger(ImapContext.class);

	public final String latd;
	public final String server;
	private final String sid;
	public final String partition;
	private Optional<PoolableStoreClient> imapClient = Optional.empty();

	protected static final Cache<String, ImapContext> sidToCtxCache = createCache();

	public final AuthUser user;

	public static class CacheRegistration implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register(ImapContext.class, sidToCtxCache);
		}
	}

	private static final Cache<String, ImapContext> createCache() {
		int maxChild = cyrusMaxChild();
		Cache<String, ImapContext> ret = Caffeine.newBuilder().expireAfterAccess(5, TimeUnit.MINUTES)
				.removalListener((String key, ImapContext value, RemovalCause cause) -> {
					value.imapClient.ifPresent(psc -> {
						logger.info("[removalListener] Closing underlying imap connection for {} ({})", key, cause);
						if (!psc.isClosed()) {
							psc.closeImpl();
						}
						value.imapClient = Optional.empty();
					});
				}).maximumSize(maxChild / 4)//
				.recordStats()//
				.build();
		VertxPlatform.getVertx().setPeriodic(TimeUnit.SECONDS.toMillis(30), tid -> ret.cleanUp());
		VertxPlatform.getVertx().setPeriodic(TimeUnit.MINUTES.toMillis(5), tid -> {
			logger.info("ImapContext CACHE: {}", ret.stats());
		});
		return ret;
	}

	private static int cyrusMaxChild() {
		try {
			return Optional.ofNullable(LocalSysconfCache.get().integerValue(SysConfKeys.imap_max_child.name()))
					.orElse(200);
		} catch (Exception e) {
			logger.warn("error loading cached sysconf {}", e.getMessage());
			return 200;
		}
	}

	private ImapContext(String latd, String partition, String imapSrv, String sid, AuthUser user) {
		this.partition = partition;
		this.latd = latd;
		this.server = imapSrv;
		this.sid = sid;
		this.user = user;
	}

	private static class PoolableStoreClient extends StoreClient {

		private final ReentrantLock lock;

		public PoolableStoreClient(String hostname, int port, String login, String password) {
			super(hostname, port, login, password, 15);
			this.lock = new ReentrantLock();
		}

		@Override
		public void close() {

		}

		public void closeImpl() {
			super.close();
		}

		@Override
		public boolean isClosed() {
			return super.isClosed();
		}

	}

	private PoolableStoreClient imapAsUser() {
		if (imapClient.isPresent() && !imapClient.get().isClosed()) {
			return imapClient.get();
		} else {
			PoolableStoreClient sc = new PoolableStoreClient(server, 1143, latd, sid);
			try {
				boolean minaClientOk = sc.login();
				if (!minaClientOk) {
					sc.closeImpl();
					throw new ServerFault("Failed to establish both imap connections for " + latd);
				}
				imapClient = Optional.of(sc);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				sc.closeImpl();
				throw new ServerFault(e);
			}
			return sc;
		}
	}

	@FunctionalInterface
	public static interface ImapClientConsumer<T> {
		T accept(StoreClient sc) throws Exception;
	}

	public <T> T withImapClient(ImapClientConsumer<T> cons) {
		try (PoolableStoreClient sc = imapAsUser()) {
			if (sc.lock.tryLock(1, TimeUnit.SECONDS)) {
				try {
					return cons.accept(sc);
				} finally {
					sc.lock.unlock();
				}
			} else {
				throw new ServerFault("[" + latd + "] Failed to grab imap con lock.");
			}
		} catch (ServerFault sf) {
			throw sf;
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

	public static ImapContext of(BmContext context) {
		String key = context.getSecurityContext().getSessionId();
		if (key == null) {
			throw new ServerFault("ImapContext requires a non null sessionId ctx: " + context.getSecurityContext());
		}
		return sidToCtxCache.get(key, k -> {
			if (logger.isDebugEnabled()) {
				logger.debug("ImapContext cache miss for key {}", key);
			}
			IServiceProvider srvProv = context.provider();
			IAuthentication authApi = srvProv.instance(IAuthentication.class);
			AuthUser curUser = authApi.getCurrentUser();
			if (curUser != null && curUser.value != null) {
				String latd = curUser.value.login + "@" + curUser.domainUid;
				String partition = CyrusPartition.forServerAndDomain(curUser.value.dataLocation,
						curUser.domainUid).name;

				ItemValue<Server> imap = Topology.get().datalocation(curUser.value.dataLocation);
				String imapSrv = imap.value.address();
				return new ImapContext(latd, partition, imapSrv, key, curUser);
			} else {
				throw new ServerFault("ImapContext is intended for users with a mailbox");
			}
		});

	}

	@Override
	public String toString() {
		return "ImapContext [latd=" + latd + ", server=" + server + ", partition=" + partition + ", user=" + user + "]";
	}

}
