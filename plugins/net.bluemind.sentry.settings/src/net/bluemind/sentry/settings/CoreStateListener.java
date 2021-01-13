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
package net.bluemind.sentry.settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.sentry.Sentry;
import io.sentry.SentryClient;
import net.bluemind.system.api.SystemState;
import net.bluemind.system.stateobserver.IStateListener;

public class CoreStateListener implements IStateListener {
	private static final Logger logger = LoggerFactory.getLogger(CoreStateListener.class);

	public void stateChanged(SystemState state) {
		if (state == SystemState.CORE_STATE_RUNNING) {
			SentryProperties sentryProps = new SentryProperties();
			if (sentryProps.enabled()) {
				logger.info("Sentry enabled");
				Sentry.close();
				SentryClient client = Sentry.init();
				ClientAccess.client = client;
			} else {
				logger.info("Sentry disabled");
				Sentry.close();
			}
		} else if (Sentry.isInitialized()) {
			logger.info("Sentry disabled");
			Sentry.close();
		}
	}
}
