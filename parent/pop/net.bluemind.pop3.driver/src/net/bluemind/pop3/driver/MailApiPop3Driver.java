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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.pop3.driver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.authentication.api.LoginResponse.Status;
import net.bluemind.config.BmIni;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.pop3.endpoint.MailboxConnection;
import net.bluemind.pop3.endpoint.PopDriver;

public class MailApiPop3Driver implements PopDriver {

	private static final Logger logger = LoggerFactory.getLogger(MailApiPop3Driver.class);

	@Override
	public MailboxConnection connect(String login, String password) {
		String url = BmIni.value("external-url");

		IServiceProvider prov = ClientSideServiceProvider.getProvider("http://" + url + ":8090", null);

		IAuthentication authapi = prov.instance(IAuthentication.class);
		LoginResponse auth = authapi.login(login, password, "pop3-endpoint");
		if (auth.status == Status.Ok) {
			prov = ClientSideServiceProvider.getProvider("http://" + url + ":8090", auth.authKey);
			logger.info("Connection established for {}", auth.authUser.value.defaultEmailAddress());
			return new CoreConnection(prov, auth.authUser);
		} else {
			logger.warn("Failed to authenticate {}", login);
			return null;
		}
	}

}
