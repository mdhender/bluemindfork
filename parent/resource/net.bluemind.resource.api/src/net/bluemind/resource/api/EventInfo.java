/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.resource.api;

import net.bluemind.core.api.BMApi;

/** Holds information about a calendar event. */
@BMApi(version = "3")
public class EventInfo {
	private String organizerUid;
	private String description;

	public EventInfo(final String description, final String organizerUid) {
		this.description = description;
		this.organizerUid = organizerUid;
	}

	public EventInfo(final String description) {
		this(description, null);
	}

	public EventInfo() {
		this(null, null);
	}

	public String getOrganizerUid() {
		return organizerUid;
	}

	public void setOrganizerUid(String organizerUid) {
		this.organizerUid = organizerUid;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
