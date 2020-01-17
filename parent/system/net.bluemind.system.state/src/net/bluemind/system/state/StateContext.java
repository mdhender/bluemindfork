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

import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonObject;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.api.SystemState;

public class StateContext {

	protected static final Logger logger = LoggerFactory.getLogger(StateContext.class);
	private static State currentState;
	private static Timer heartbeatTimer;

	static {
		currentState = new StartingState();
	}

	public static void start() {
		JsonObject stateObject = new JsonObject().put("operation", currentState.getSystemState().operation());
		publishOperation(stateObject);

		// BM-12022: re-publish our active state periodically to ensure we don't
		// have a component stuck in maintenance state. StateObserverVerticle
		// only does something when the new state is different from the old
		// state.
		heartbeatTimer = new Timer("core-heartbeat-timer", true);
		heartbeatTimer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				logger.info("Core state heartbeat : {}", getState().operation());
				publishOperation(new JsonObject().put("operation", getState().operation()));
			}

		}, 4000L, 4000L);
	}

	/**
	 * in-plugin handler exists in {@link StateBroadcastingVerticle}
	 * 
	 * @param stateObject
	 */
	private static void publishOperation(JsonObject stateObject) {
		VertxPlatform.getVertx().eventBus().publish(SystemState.BROADCAST, stateObject);
	}

	public static SystemState getState() {
		return currentState.getSystemState();
	}

	public static void setState(String operation) {
		logger.info("Core state transition from {} to {}", currentState.getSystemState().operation(), operation);
		JsonObject stateObject = new JsonObject();
		stateObject.put("previousState", currentState.getSystemState().name());
		currentState = currentState.stateChange(operation);
		stateObject.put("operation", currentState.getSystemState().operation());
		publishOperation(stateObject);
	}

}
