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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationObserver;

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
		if (oldendpoint != null && sentryendpoint != null && !oldendpoint.equals(sentryendpoint)) {
			/* Reconfiguration needed */
			logger.info("Sentry reconfiguration needed. dsn changed from {} to {}", oldendpoint, sentryendpoint);
			MQ.getProducer(Topic.SENTRY_CONFIG).send(SentryConfiguration.get(sentryendpoint));
		}
	}
}
