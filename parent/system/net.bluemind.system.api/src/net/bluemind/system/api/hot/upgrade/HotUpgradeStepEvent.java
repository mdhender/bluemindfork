/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.system.api.hot.upgrade;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class HotUpgradeStepEvent {

	public String step;
	public Status status;
	public String message;
	public long date;

	public HotUpgradeStepEvent() {
		date = System.currentTimeMillis();
	}

	public static HotUpgradeStepEvent create(String step, Status status, String message) {
		HotUpgradeStepEvent evt = new HotUpgradeStepEvent();
		evt.step = step;
		evt.status = status;
		evt.message = message;
		return evt;
	}

	@BMApi(version = "3")
	public enum Status {
		NOT_STARTED, SUCCESS, ERROR, IN_PROGRESS
	}

}
