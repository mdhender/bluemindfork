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
package net.bluemind.core.password.bruteforce;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.bluemind.authentication.provider.IAuthProvider;
import net.bluemind.authentication.provider.ILoginValidationListener;
import net.bluemind.core.api.fault.ServerFault;

public class Fail2Ban implements ILoginValidationListener, IAuthProvider {
	private static final Logger logger = LoggerFactory.getLogger(Fail2Ban.class);

	private static final Cache<String, AtomicInteger> trials = CacheBuilder.newBuilder()
			.expireAfterAccess(20, TimeUnit.SECONDS).build();

	public Fail2Ban() {
	}

	@Override
	public void onValidLogin(IAuthProvider provider, boolean userExists, String userLogin, String domain,
			String password) {
		trials.invalidate(userLogin + "@" + domain);
	}

	@Override
	public AuthResult check(IAuthContext authContext) throws ServerFault {
		String latd = authContext.getRealUserLogin() + "@" + authContext.getDomain().value.name;
		AtomicInteger authCount = trials.getIfPresent(latd);
		if (authCount == null) {
			logger.debug("First attempt for {}", latd);
			trials.put(latd, new AtomicInteger(1));
			return AuthResult.UNKNOWN;
		} else {
			int val = authCount.incrementAndGet();
			if (val > 3) {
				logger.warn("Too many ({}) attempts for {}/{}. Wait 20sec to retry", val, latd,
						authContext.getSecurityContext().getRemoteAddresses());
				return AuthResult.NO;
			} else {
				logger.info("** Attempt {} for {}", val, latd);
				return AuthResult.UNKNOWN;
			}
		}
	}

	@Override
	public int priority() {
		return Integer.MAX_VALUE;
	}

}
