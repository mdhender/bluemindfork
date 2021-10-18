/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.cti.wazo.api.client;

import com.google.common.base.Strings;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.cti.wazo.api.client.connection.HttpsWazoApiConnection;
import net.bluemind.cti.wazo.api.client.exception.WazoConnectionException;
import net.bluemind.cti.wazo.config.WazoEndpoints;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;

public class WazoApiClient {

	protected String host;
	private HttpsWazoApiConnection connection;

	public WazoApiClient(String domainUid) {
		connection = new HttpsWazoApiConnection();
		setWazoApiHost(domainUid);
	}

	private void setWazoApiHost(String domainUid) {

		host = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomainSettings.class, domainUid)
				.get().get(DomainSettingsKeys.cti_host.name());

		if (Strings.isNullOrEmpty(host)) {
			throw new WazoConnectionException("Unknown Wazo API host");
		}
	}

	public HttpsWazoApiConnection getConnection(WazoEndpoints endpoint) {
		connection.init(host, endpoint);
		return connection;
	}
}
