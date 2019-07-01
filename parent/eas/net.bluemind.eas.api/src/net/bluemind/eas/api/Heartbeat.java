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
package net.bluemind.eas.api;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class Heartbeat {

	public String deviceUid;
	public Long value;

	public Heartbeat() {

	}

	@Override
	public String toString() {
		return "device uid: " + deviceUid + ", heartbeat: " + value;
	}

}
