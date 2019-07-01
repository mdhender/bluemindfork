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
package net.bluemind.proxy.http.impl.vertx;

import org.vertx.java.core.Vertx;

import net.bluemind.system.api.SystemState;

public class CoreState {

	public CoreState(Vertx vertx) {
	}

	public void start() {
	}

	public boolean ok() {
		return state() == SystemState.CORE_STATE_RUNNING;
	}

	public boolean notRunning() {
		return state() == SystemState.CORE_STATE_UNKNOWN;
	}

	public boolean needUpgrade() {
		return state() == SystemState.CORE_STATE_UPGRADE;
	}

	public boolean maintenace() {
		return state() != SystemState.CORE_STATE_RUNNING;
	}

	public boolean notInstalled() {
		return state() == SystemState.CORE_STATE_NOT_INSTALLED;
	}

	public SystemState state() {
		return HpsCoreListener.state;
	}

}
