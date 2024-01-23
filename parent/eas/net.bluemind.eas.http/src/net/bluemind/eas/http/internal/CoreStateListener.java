/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.eas.http.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eas.dto.EasBusEndpoints;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.api.SystemState;
import net.bluemind.system.stateobserver.IStateListener;

public class CoreStateListener implements IStateListener {
	private static final Logger logger = LoggerFactory.getLogger(CoreStateListener.class);

	public static SystemState state;

	@Override
	public void stateChanged(SystemState newState) {
		logger.info("New state is {}", newState.name());
		state = newState;
		VertxPlatform.eventBus().publish(EasBusEndpoints.PURGE_SESSIONS, (Object) null);
		VertxPlatform.eventBus().publish(EasBusEndpoints.PUSH_UNREGISTRATION, (Object) null);
	}
}
