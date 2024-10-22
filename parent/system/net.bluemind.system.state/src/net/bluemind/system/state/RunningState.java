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

import net.bluemind.system.api.SystemState;

public class RunningState extends State {

	@Override
	public State stateChange(String operation) {
		switch (operation) {
		case "core.started":
		case "core.cloning.end":
			return this;
		case "core.upgrade.start":
			return new MaintenanceUpgradeState();
		case "core.maintenance.start":
			return new MaintenanceState();
		case "core.demote.start":
			return new DemoteState();
		default:
			return super.stateChange(operation);
		}
	}

	@Override
	public SystemState getSystemState() {
		return SystemState.CORE_STATE_RUNNING;
	}

}
