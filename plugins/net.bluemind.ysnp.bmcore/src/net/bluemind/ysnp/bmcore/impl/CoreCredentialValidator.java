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
package net.bluemind.ysnp.bmcore.impl;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.IAuthenticationAsync;
import net.bluemind.authentication.api.ValidationKind;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.http.HttpClientFactory;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.Server;
import net.bluemind.ysnp.AuthConfig;
import net.bluemind.ysnp.ICredentialValidator;

public class CoreCredentialValidator implements ICredentialValidator {

	private static final Logger logger = LoggerFactory.getLogger(CoreCredentialValidator.class);

	// Use this realm if login doesn't contain '@' and no or empty realm
	// specified
	private static final String DEFAULT_REALM = "global.virt";

	@Override
	public Kind validate(String username, String credential, String realm, String service, AuthConfig authConfig) {

		String host = "127.0.0.1";
		try {
			host = Topology.get().core().value.address();
		} catch (Exception e) {
			Server core = new Server();
			core.ip = "127.0.0.1";
			core.tags = Arrays.asList("bm/core");
			Topology.update(Arrays.asList(ItemValue.create("fake-srv", core)));
		}

		if (host == null) {
			logger.warn("bm/core not found for " + username, "trying 127.0.0.1");
			host = "127.0.0.1";
		}

		String url = "http://" + host + ":8090";
		if (logger.isDebugEnabled()) {
			logger.debug("trying " + username + " / " + credential + ", " + url);
		}

		IAuthentication authClient = HttpClientFactory.create(IAuthentication.class, IAuthenticationAsync.class, url)
				.syncClient((String) null);

		String latd = username;
		if (realm != null && !realm.trim().equals("") && !username.contains("@")) {
			latd = username + "@" + realm;
		} else if (!username.contains("@")) {
			latd = username + "@" + DEFAULT_REALM;
		}

		Kind vk = Kind.No;
		ValidationKind lr = null;
		try {
			lr = authClient.validate(latd, credential, "ysnp");
			if (lr == ValidationKind.TOKEN) {
				vk = Kind.Token;
			} else if (lr == ValidationKind.PASSWORD) {
				vk = Kind.Password;
			} else if (authConfig.expiredOk && lr == ValidationKind.PASSWORDEXPIRED) {
				vk = Kind.Password;
			} else if (authConfig.archivedOk && lr == ValidationKind.ARCHIVED) {
				vk = Kind.Password;
			} else {
				logger.debug("could not validate {}/{}", latd, credential);
				vk = Kind.No;
			}
		} catch (Exception e) {
			logger.error("error validating login {} : {}", latd, e.getMessage(), e);
		}

		logger.info("validate response for (username :{}, realm :{}, service:{},) : {} - core response: {}", username,
				realm, service, vk, lr);
		return vk;

	}
}
