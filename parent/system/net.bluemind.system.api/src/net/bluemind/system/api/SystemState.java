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
package net.bluemind.system.api;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public enum SystemState {

	CORE_STATE_STARTING("core.state.starting"), //
	CORE_STATE_RUNNING("core.state.running"), //
	CORE_STATE_MAINTENANCE("core.state.maintenance"), //
	CORE_STATE_NOT_INSTALLED("core.state.not.installed"), //
	CORE_STATE_UPGRADE("core.state.upgrade"), //
	CORE_STATE_CLONING("core.state.cloning"), //
	CORE_STATE_STOPPING("core.state.stopping"), //
	CORE_STATE_UNKNOWN("core.state.unkwown");

	private String operation;

	/**
	 * EventBus address used for state broadcasting
	 */
	public static final String BROADCAST = "core.status.broadcast";

	private SystemState(String operation) {
		this.operation = operation;
	}

	public String operation() {
		return operation;
	}

	public static SystemState fromOperation(String op) {
		for (SystemState v : values()) {
			if (v.operation().equals(op)) {
				return v;
			}
		}
		throw new IllegalArgumentException("unknown op " + op);
	}

}
