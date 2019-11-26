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
package net.bluemind.system.schemaupgrader.runner;

import java.util.List;

import net.bluemind.system.persistance.SchemaVersion;
import net.bluemind.system.persistance.SchemaVersion.UpgradePhase;

public class UpdaterFilter {

	private final int releaseMajor;
	private final int releaseStart;
	private final int releaseEnd;
	private final List<SchemaVersion> storedUpgraders;
	private String component;

	public UpdaterFilter(int major, int start, int end, List<SchemaVersion> storedUpgraders, String component) {
		releaseMajor = major;
		releaseStart = start;
		releaseEnd = end;
		this.storedUpgraders = storedUpgraders;
		this.component = component;
	}

	public boolean accept(int major, int release, String component, UpgradePhase phase) {
		if (!component.equals(this.component)) {
			return false;
		}
		if (upgraderAlreadyPassed(major, release, component, phase)) {
			return false;
		}
		return checkVersion(major, release);
	}

	private boolean upgraderAlreadyPassed(int major, int release, String component, UpgradePhase phase) {
		SchemaVersion thisUpgrader = new SchemaVersion(major, release).component(component).phase(phase);
		for (SchemaVersion schemaVersion : storedUpgraders) {
			if (thisUpgrader.equals(schemaVersion) && schemaVersion.success) {
				return true;
			}
		}
		return false;
	}

	private boolean checkVersion(int major, int release) {
		if (major != releaseMajor) {
			return false;
		}
		if (release > releaseStart && release <= releaseEnd) {
			return true;
		}
		return false;
	}
}
