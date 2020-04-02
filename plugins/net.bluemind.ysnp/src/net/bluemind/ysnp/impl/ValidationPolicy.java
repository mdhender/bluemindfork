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

import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.ysnp.ICredentialValidator;
import net.bluemind.ysnp.ICredentialValidator.Kind;
import net.bluemind.ysnp.ICredentialValidatorFactory;
import net.bluemind.ysnp.YSNPConfiguration;

public class ValidationPolicy {

	private List<ICredentialValidatorFactory> validatorsFactories;
	private Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * key: token, value: login@domain
	 */
	private Cache<String, String> tokenCache;

	private TokenCacheSync tokenSync;

	public ValidationPolicy(YSNPConfiguration conf) {
		RunnableExtensionLoader<ICredentialValidatorFactory> rel = new RunnableExtensionLoader<ICredentialValidatorFactory>();
		List<ICredentialValidatorFactory> factories = rel.loadExtensions("net.bluemind.ysnp",
				"credentialvalidatorfactory", "credential_validator_factory", "implementation");

		Collections.sort(factories, new ValidatorsComparator());
		validatorsFactories = factories;
		for (ICredentialValidatorFactory cvf : validatorsFactories) {
			cvf.init(conf);
		}
		int cores = Runtime.getRuntime().availableProcessors();
		int conc = Math.max(4, cores);
		tokenCache = CacheBuilder.newBuilder().concurrencyLevel(conc).recordStats().initialCapacity(1024)
				.expireAfterAccess(2, TimeUnit.MINUTES).build();
		this.tokenSync = new TokenCacheSync();
		tokenSync.start(tokenCache);

		TimerTask stats = new TimerTask() {

			@Override
			public void run() {
				logger.info(tokenCache.stats().toString());
			}
		};
		Timer t = new Timer();
		t.schedule(stats, 30000, 30000);
	}

	public boolean validate(String login, String password, String service, String realm) {
		String latd = login + "@" + realm;

		String cachedLatd = tokenCache.getIfPresent(password);
		if (cachedLatd != null && cachedLatd.equals(latd)) {
			logger.info("Access to {} granted from token cache for {}", service, latd);
			return true;
		}

		boolean ret = false;
		long time = System.currentTimeMillis();
		for (ICredentialValidatorFactory cvf : validatorsFactories) {
			ICredentialValidator validator = cvf.getValidator();
			Kind vk = validator.validate(login, password, realm, service);
			if (vk != null && vk != Kind.No) {
				logger.info("Access to service {} granted to {} with '{}' validator in {}ms.", service, login,
						cvf.getName(), (System.currentTimeMillis() - time));
				ret = true;

				if (vk == Kind.Token) {
					tokenCache.put(password, latd);
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
