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
package net.bluemind.sentry.settings.core;

import io.vertx.core.json.JsonObject;
import net.bluemind.config.InstallationId;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.system.api.IInstallation;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.InstallationVersion;
import net.bluemind.system.api.SubscriptionInformations;
import net.bluemind.system.api.SystemConf;

public class SentryConfiguration {

	private SentryConfiguration() {
	}

	public static JsonObject get() {
		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		ISystemConfiguration confService = provider.instance(ISystemConfiguration.class);
		SystemConf sysconf = confService.getValues();
		return get(sysconf.stringValue("sentry_endpoint"));
	}

	public static JsonObject get(String sentryDsn) {
		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IInstallation instService = provider.instance(IInstallation.class);

		SubscriptionInformations sub = instService.getSubscriptionInformations();
		InstallationVersion version = instService.getVersion();

		JsonObject msg = new JsonObject().put("dsn", sentryDsn);
		if (sub.customerCode != null && !sub.customerCode.isEmpty()) {
			msg.put("environment", sub.customerCode);
		}
		if (version.softwareVersion != null && !version.softwareVersion.isEmpty()) {
			msg.put("release", version.softwareVersion);
		}
		if (InstallationId.getIdentifier() != null && !InstallationId.getIdentifier().isEmpty()) {
			msg.put("servername", InstallationId.getIdentifier());
		}
		return msg;
	}
}
