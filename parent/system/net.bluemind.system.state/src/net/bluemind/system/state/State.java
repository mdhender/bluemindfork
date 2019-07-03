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
package net.bluemind.system.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.api.SystemState;

public abstract class State {

	protected static final Logger logger = LoggerFactory.getLogger(State.class);

	public State stateChange(String operation) {
		switch (operation) {
		case "core.stopped":
			return new StoppingState();
		case "reset":
			return new StartingState();
		default:
			logger.warn("State ({}) {} cannot handle transition {}", this.getClass().getSimpleName(),
					StateContext.getState().name(), operation);
			throw new ServerFault("transition is not possible");
		}
	}

	public abstract SystemState getSystemState();
}
