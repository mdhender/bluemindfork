/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2020
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

import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationObserver;
import net.bluemind.system.nginx.NginxService;

/**
 * This observer is called inside the core, and redistribute sentry
 * configuration through the bluemind message queue globally
 */

public class SentrySysconfObserver implements ISystemConfigurationObserver {
	private Logger logger = LoggerFactory.getLogger(SentrySysconfObserver.class);

	@Override
	public void onUpdated(BmContext context, SystemConf previous, SystemConf current) throws ServerFault {
		String sentryendpoint = current.stringValue(SysConfKeys.sentry_endpoint.name());
		String oldendpoint = previous.stringValue(SysConfKeys.sentry_endpoint.name());

		String sentrywebendpoint = current.stringValue(SysConfKeys.sentry_web_endpoint.name());
		String oldwebendpoint = previous.stringValue(SysConfKeys.sentry_web_endpoint.name());

		String sentrywebhostname = "";
		String sentrywebport = "";
		if (sentrywebendpoint != null && !sentrywebendpoint.isEmpty()) {
			try {
				URL url = new URL(sentrywebendpoint);
				sentrywebhostname = url.getHost();
				sentrywebport = String.valueOf(url.getPort() == -1 ? url.getDefaultPort() : url.getPort());
			} catch (MalformedURLException e) {
				logger.error("incorrect sentry webhostname set", e);
			}
		}

		if ((oldendpoint == null && sentryendpoint != null) || (oldwebendpoint == null && sentrywebendpoint != null)
				|| (oldendpoint != null && sentryendpoint != null && !oldendpoint.equals(sentryendpoint))
				|| (oldwebendpoint != null && sentrywebendpoint != null && !oldwebendpoint.equals(sentrywebendpoint))) {
			/* Reconfiguration needed */
			logger.info("Sentry reconfiguration needed. dsn changed from {} to {} or web dsn from {} to {}",
					oldendpoint, sentryendpoint, oldwebendpoint, sentrywebendpoint);
			MQ.getProducer(Topic.SENTRY_CONFIG).send(SentryConfiguration.get(sentryendpoint, sentrywebendpoint));
			NginxService nginxService = new NginxService();
			nginxService.updateSentryUpstream(sentrywebhostname.isEmpty() ? "localhost" : sentrywebhostname,
					sentrywebport);
		}
	}
}
