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
package net.bluemind.ysnp.bmcore.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.system.api.SystemState;
import net.bluemind.system.stateobserver.IStateListener;

public class CoreStateListener implements IStateListener {

	private static final Logger logger = LoggerFactory.getLogger(CoreStateListener.class);
	private static final String PERMIT = "200 PERMIT\n";
	private static final String REJECT = "200 DEFER Service unavailable\n";
	public static String current = REJECT;

	@Override
	public void stateChanged(SystemState newState) {
		logger.info("new state is {}", newState);
		current = (newState == SystemState.CORE_STATE_RUNNING) ? PERMIT : REJECT;
	}

}
