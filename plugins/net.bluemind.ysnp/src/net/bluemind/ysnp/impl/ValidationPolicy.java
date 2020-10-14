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
package net.bluemind.ysnp.impl;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.ysnp.ICredentialValidator;
import net.bluemind.ysnp.ICredentialValidator.Kind;
import net.bluemind.ysnp.ICredentialValidatorFactory;
import net.bluemind.ysnp.YSNPConfiguration;

public class ValidationPolicy {
	private static final Logger logger = LoggerFactory.getLogger(ValidationPolicy.class);
	private static final HashFunction hash = Hashing.goodFastHash(32);
	private final List<ICredentialValidatorFactory> validatorsFactories;

	/**
	 * key: token, value: login@domain
	 */
	private static final Cache<String, String> tokenCache = CacheBuilder.newBuilder().recordStats()
			.initialCapacity(1024).expireAfterAccess(10, TimeUnit.MINUTES).build();
	/**
	 * key: login@domain, value: last valid password
	 */
	private static final Cache<String, String> pwCache = CacheBuilder.newBuilder().recordStats().initialCapacity(1024)
			.expireAfterAccess(10, TimeUnit.MINUTES).build();

	private TokenCacheSync tokenSync;

	public static class CacheRegistration implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register("ysnp-validationpolicy-token", tokenCache);
			cr.register("ysnp-validationpolicy-password", pwCache);
		}
	}

	public ValidationPolicy(YSNPConfiguration conf) {
		RunnableExtensionLoader<ICredentialValidatorFactory> rel = new RunnableExtensionLoader<>();
		List<ICredentialValidatorFactory> factories = rel.loadExtensions("net.bluemind.ysnp",
				"credentialvalidatorfactory", "credential_validator_factory", "implementation");

		Collections.sort(factories, new ValidatorsComparator());
		validatorsFactories = factories;
		for (ICredentialValidatorFactory cvf : validatorsFactories) {
			cvf.init(conf);
		}

		this.tokenSync = new TokenCacheSync();
		tokenSync.start(tokenCache, pwCache);

		TimerTask stats = new TimerTask() {
			@Override
			public void run() {
				if (logger.isInfoEnabled()) {
					logger.info("tokens {}", tokenCache.stats());
					logger.info("passwords {}", pwCache.stats());
				}
			}
		};

		Timer t = new Timer();
		t.schedule(stats, 30000, 30000);
	}

	public boolean validate(String login, String password, String service, String realm, boolean expireOk) {
		String latd = login + "@" + realm;

		String cachedLatd = tokenCache.getIfPresent(password);
		if (cachedLatd != null && cachedLatd.equals(latd)) {
			logger.info("Access to {} granted from token cache for {}", service, latd);
			return true;
		}
		String cachedPw = pwCache.getIfPresent(latd);
		if (cachedPw != null && cachedPw.equals(hash.hashString(password, StandardCharsets.UTF_8).toString())) {
			logger.info("Access to {} granted from pw cache for {}", service, latd);
			return true;
		}

		boolean ret = false;
		long time = System.currentTimeMillis();
		for (ICredentialValidatorFactory cvf : validatorsFactories) {
			ICredentialValidator validator = cvf.getValidator();
			Kind vk = validator.validate(login, password, realm, service, expireOk);
			if (vk != null && vk != Kind.No) {
				logger.info("Access to service {} granted to {} with '{}' validator in {}ms.", service, login,
						cvf.getName(), (System.currentTimeMillis() - time));
				ret = true;
				if (vk == Kind.Token) {
					tokenCache.put(password, latd);
				} else {
					pwCache.put(latd, hash.hashString(password, StandardCharsets.UTF_8).toString());
				}
				break;
			}
		}

		if (!ret) {
			logger.warn("all {} validator(s) rejected {} in {}ms.", validatorsFactories.size(), login,
					(System.currentTimeMillis() - time));
		}

		return ret;
	}
}
