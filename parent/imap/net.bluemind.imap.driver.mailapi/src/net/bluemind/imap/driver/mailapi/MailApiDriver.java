/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.imap.driver.mailapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.authentication.api.AuthUser;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.authentication.api.LoginResponse.Status;
import net.bluemind.config.BmIni;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.imap.endpoint.driver.MailboxConnection;
import net.bluemind.imap.endpoint.driver.MailboxDriver;

public class MailApiDriver implements MailboxDriver {

	private static final Logger logger = LoggerFactory.getLogger(MailApiDriver.class);

	// private static final String coreUrl =
	// "http://meae-core-2.dev.bluemind.net:8090";
	private IServiceProvider prov;
	private IAuthentication anonAuthApi;

	private String coreUrl;

	public MailApiDriver() {
		String host = BmIni.value("external-url");
		this.coreUrl = "http://" + host + ":8090";
		this.prov = ClientSideServiceProvider.getProvider(coreUrl, null);
		this.anonAuthApi = prov.instance(IAuthentication.class);
	}

	@Override
	public MailboxConnection open(String ak, String sk) {
		LoginResponse login = anonAuthApi.login(ak, sk, "imap-endpoint");
		if (login.status != Status.Ok) {
			logger.warn("Got status {}", login.status);
			return null;
		}
		ClientSideServiceProvider userProv = ClientSideServiceProvider.getProvider(coreUrl, login.authKey);
		AuthUser current = userProv.instance(IAuthentication.class).getCurrentUser();
		logger.info("[{}] logged-in.", current.value.defaultEmail());
		return new MailApiConnection(userProv, current);
	}

}
