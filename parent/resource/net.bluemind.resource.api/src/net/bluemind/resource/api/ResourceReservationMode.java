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
package net.bluemind.resource.api;

import net.bluemind.core.api.BMApi;

/**
 * Different ways to reserve a resource
 */
@BMApi(version = "3")
public enum ResourceReservationMode {
	/**
	 * Resource's participation status remains "pending" until the manager accepts
	 * or rejects the invitation.
	 */
	OWNER_MANAGED,

	/**
	 * Resource manager receives booking requests and participation is confirmed
	 * automatically if the resource is available for the requested timeslot (within
	 * working hours and no other booking confirmed).
	 */
	AUTO_ACCEPT,

	/**
	 * This mode acts exactly as { {@link #AUTO_ACCEPT } for acceptance. However,
	 * with this mode, the resource manager receives booking requests and
	 * participation is rejected automatically if the resource is not available for
	 * the requested timeslot.
	 */
	AUTO_ACCEPT_REFUSE
}
