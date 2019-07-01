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
package net.bluemind.core.rest.http;

import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.locator.client.LocatorClient;

public class RemoteServiceProvider {

	private LocatorClient locatorClient = new LocatorClient();
	private String login;
	private String apiey;

	public RemoteServiceProvider(String loginAtDomain, String apiKey) {
		this.login = loginAtDomain;
		this.apiey = apiKey;
	}

	public static RemoteServiceProvider auth(String loginAtDomain, String apiKey) {
		return new RemoteServiceProvider(loginAtDomain, apiKey);
	}

	public IServiceProvider provider(String tag) {

		String host = locatorClient.locateHost(tag, login);
		String base = "http://" + host + ":8090";
		return ClientSideServiceProvider.getProvider(base, apiey);
	}
}
